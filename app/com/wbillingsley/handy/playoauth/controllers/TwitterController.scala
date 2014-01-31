package com.wbillingsley.handy.playoauth.controllers

import play.api.libs.oauth.{OAuth, OAuthCalculator, ConsumerKey, ServiceInfo, RequestToken}
import play.api.mvc.{Controller, Action, Request, RequestHeader, EssentialAction}
import play.api.libs.ws.WS
import com.wbillingsley.handy.{Ref, RefFuture, Refused, RefFailed}
import com.wbillingsley.handy.playoauth._
import Ref._
import play.api.Play
import Play.current
import com.wbillingsley.handy.playoauth.PlayAuth

import play.api.libs.concurrent.Execution.Implicits.defaultContext


/**
 * Handles Twitter log-in. Based on sample code from Play Framework 
 * documentation.
 */
object TwitterController extends Controller with OAuthController {
  
  val key = for (
      ck <- Play.configuration.getString("auth.twitter.ckey"); 
      s <- Play.configuration.getString("auth.twitter.csecret")
  ) yield ConsumerKey(ck, s)
  
  case object Twitter extends Service {
    val name = "Twitter"
    def available = key.isDefined 
  }
  def service = Twitter
    
  val oAuthOpt = for (k <- key) yield OAuth(ServiceInfo(
    "https://api.twitter.com/oauth/request_token",
    "https://api.twitter.com/oauth/access_token",
    "https://api.twitter.com/oauth/authorize", k),
    false
  )
    
  val TOKENNAME = "twittertoken"
  val SECRETNAME = "twittersecret"

  /**
   * Beginning of the Sign in with Twitter flow, using OAuth1.
   * 
   */
  def requestAuth = Action { implicit request =>
    oAuthOpt match {
      case Some(oAuth) => { 
        oAuth.retrieveRequestToken(routes.TwitterController.callback.absoluteURL()) match {
          case Right(t) => {
            // We received the unauthorized tokens in the OAuth object - store it before we proceed
            Redirect(oAuth.redirectUrl(t.token)).withSession(request.session + (TOKENNAME -> t.token) + (SECRETNAME -> t.secret))
          }
          case Left(e) => throw e
        }
      }
      case _ => InternalServerError("This server's client key and secret for Twitter have not been set")
    }
  }  
  
  /**
   * Twitter redirects the user back to this action upon authorization
   */
  def callback = EssentialAction { implicit request =>
    
    /**
     * Finds a request or access token from the request, if there is one
     */
    def sessionTokenPair(implicit request: RequestHeader): Ref[RequestToken] = {    
      Ref(for {
        token <- request.session.get(TOKENNAME)
        secret <- request.session.get(SECRETNAME)
      } yield {
        RequestToken(token, secret)
      })
    }    
    
    /**
     * Given an authentication token, goes and looks up that user's details
     */
    def userFromAuth(token: RequestToken) = {
      
      for {
        k <- key.toRef orIfNone new IllegalStateException("This server's client key and secret for Twitter have not been set")
        ws = WS.url("https://api.twitter.com/1.1/account/verify_credentials.json").sign(OAuthCalculator(k, token)).get()
        resp <- new RefFuture(ws)
        json = resp.json
        id <- (json \ "id_str").asOpt[String]
      } yield {
        OAuthDetails(
          userRecord = UserRecord(
            service = "twitter",
            id = id,
            name = (json \ "name").asOpt[String],
            nickname = (json \ "screen_name").asOpt[String],
            username = (json \ "screen_name").asOpt[String],
            avatar = (json \ "profile_image_url").asOpt[String]
          ),
          raw = Some(json)
        )
      }
    }      
    
    // Fetch the user data from Twitter
    val refMem = for {
      verifier <- request.getQueryString("oauth_verifier").toRef orIfNone AuthFailed("Twitter did not provide a verification code")
      oAuth <- oAuthOpt.toRef orIfNone AuthFailed("This server's client key and secret for Twitter have not been set")
      tokenPair <- sessionTokenPair(request)
      accessToken <- oAuth.retrieveAccessToken(tokenPair, verifier) match {
        case Right(t) => t.itself
        case Left(e) => RefFailed(AuthFailed("Twitter did not provide an access token"))
      };
      mem <- userFromAuth(accessToken) orIfNone AuthFailed("Twitter did not provide any user data for that login")
    } yield mem
        
    PlayAuth.onAuthR(refMem)(request)
  }  

  /**
   * Calculates the Twitter ID from an access token
   */
  def idFromToken(r:RequestToken) = r.token.split("-")(0)

}