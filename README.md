# Handy Play OAuth

Handy-Play-OAuth is a very simple social login library for Scala [Play](http://playframework.com) applications.

It just handles the OAuth, and then gives the user data to whatever Action method you choose.  

The latest version supports:

* Twitter
* GitHub



## Getting handy-play-oauth

Handy-play-oauth can be added as a dependency to your Play app.

The dependency for Play 2.2 is

    com.wbillingsley %% handy-play-oauth % 0.2-SNAPSHOT

It's published to Sonatypes's snapshot repository, so you'll need to add a resolver for it. You can do that by adding this to your `build.sbt` file:

    resolvers += Resolver.sonatypeRepo("snapshots")


## How to use it

This library will perform the OAuth, and then hand the details off to a method you write. Writing your method is just like writing a controller method. It will be passed the OAuth information in a `Try[OAuthDetails]`.

    def onAuth(loginData:Try[OAuthDetails]) = Action { 
      implicit request => 
    
      // Do your stuff here. The OAuth info is in loginData
      
    }

Then tell Handy-Play-OAuth about it:

    import com.wbillingsley.handy.playoauth.PlayAuth
    
    PlayAuth.onAuth = MyController.onAuth


Next set up your routes. You can just forward a subpath to them.

    ->  /oauth   handyplayoauth.Routes

This gives you paths:

* `/oauth/twitter` to start sign in with Twitter
* `/oauth/github` to start sign in with GitHub
* etc.


By default, the routes are `POST` routes, rather than `GET` routes. But if you want to enable `GET` routes as well, you can:

    PlayAuth.allowGet = true

And the last thing we need to do is set the client keys and secrets for the different services. Handy Play OAuth looks for configuration settings (which you can set in `application.conf`).  The settings it looks for are:

* auth.github.ckey
* auth.github.csecret
* auth.twitter.ckey
* auth.twitter.csecret
* *etc*

Typically, you'd set those to environment variables, so you can use different keys and secrets on your development machine than in production:

    auth.twitter.ckey =    ${?MYAPP_AUTH_TWITTER_CKEY}
    auth.twitter.csecret = ${?MYAPP_AUTH_TWITTER_CSECRET}
    auth.github.ckey =     ${?MYAPP_AUTH_GITHUB_CKEY}
    auth.github.csecret =  ${?MYAPP_AUTH_GITHUB_CSECRET}
    

Congratulations, you're ready to go.


## Getting a list of configured services

You can get a list of configured services (those where you've set the client key and secret) by calling:

    PlayAuth.enabledServices



