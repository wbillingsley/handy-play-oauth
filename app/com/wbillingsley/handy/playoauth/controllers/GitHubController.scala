package com.wbillingsley.handy.playoauth.controllers

import play.api.mvc.{Controller, Action, Request}
import play.api.libs.ws.WS
import play.api.Play
import Play.current
import com.wbillingsley.encrypt.Encrypt
import com.wbillingsley.handy.{Ref, RefFuture, Refused}
import com.wbillingsley.handy.playoauth._
import Ref._
import play.Logger
import com.wbillingsley.handy.playoauth.PlayAuth

/**
 * Implements log in with the GitHub API
 */
object GitHubController extends Controller {

  val clientKey = Play.configuration.getString("auth.github.ckey").getOrElse("No key")
  val secret = Play.configuration.getString("auth.github.csecret").getOrElse("No secret")  
  
  /**
   * Beginning of the Sign in with GitHub flow, using OAuth2.
   * A random state is set in the session, and then the user is redirected to the GitHub
   * sign-in endpoint. 
   */
  def requestAuth = Action { implicit request =>    
    val randomString = Encrypt.genSaltB64
    
    val returnUrl = ""
    
    Redirect(
        "https://github.com/login/oauth/authorize", 
        Map(
          "state" -> Seq(randomString),
          "client_id" -> Seq(clientKey)  
        ), 
        303
    ).withSession(request.session + ("oauth_state" -> randomString))
  } 
  
  def callback = Action { implicit request =>    
    
    import play.api.libs.concurrent.Execution.Implicits._
    
    val stateFromSession = request.session.get("oauth_state")
    val stateFromRequest = request.getQueryString("state")

    /**
     * Calls GitHub to swap a code for an auth_token
     */
    def authTokenFromCode(code:String):Ref[String] = {
      val ws = WS.url("https://github.com/login/oauth/access_token").
    		  		withHeaders("Accept" -> "application/json").
    		  		post(Map(
				        "code" -> Seq(code),
				        "client_id" -> Seq(clientKey),
				        "client_secret" -> Seq(secret)
				    ))
	  val authToken = for (
	      resp <- new RefFuture(ws);
	      
	      tok <- { println(resp.json); (resp.json \ "access_token").asOpt[String] }
	  ) yield tok
				    
	  authToken
    }
    
    /**
     * Given an authentication token, goes and looks up that user's details on GitHub.
     * These are filled into an "Interstitial Memory" -- details to remember during the display
     * of the confirmation page.
     */
    def userFromAuth(authToken:String) = {
      val ws = WS.url("https://api.github.com/user").
    		  		withHeaders(
    		  		  "Accept" -> "application/json",
    		  		  "Authorization" -> ("token " + authToken)
    		  		).get()
      
      for (
        resp <- new RefFuture(ws);
        json = resp.json;
        id <- (resp.json \ "id").asOpt[Int].map(_.toString)
      ) yield {
        UserRecord(
            service = "github",
            id = id,
            name = (json \ "name").asOpt[String],
            nickname = (json \ "login").asOpt[String],
            username = (json \ "login").asOpt[String],
            avatar = (json \ "avatar_url").asOpt[String],
            raw = Some(json)
          )
      }
    }    
    
    val refMem = for (
      code <- Ref(request.getQueryString("code")) orIfNone Refused("GitHub provided no code");
      authToken <- authTokenFromCode(code) orIfNone Refused("GitHub did not provide an authorization token");
      mem <- userFromAuth(authToken) orIfNone Refused("GitHub did not provide any user data for that login")
    ) yield mem
    
    PlayAuth.onAuth(refMem)
  }
  
}