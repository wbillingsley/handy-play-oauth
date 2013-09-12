# Handy Play OAuth

Handy-Play-OAuth is a very simple social login library for Scala [Play](http://playframework.com) applications.

It just handles the OAuth, and then gives the user data to whatever Action method you choose.  


## Getting handy-play-oath

Handy-play-oauth can be added as a dependency to your Play app.

The dependency for Play 2.1 is

    com.wbillingsley %% handy-play-oauth % 0.1

You'll need to add a resolver to a repository where it is published. Currently, that means adding to your Play project settings

    resolvers ++= Seq(
        "handy releases" at "https://bitbucket.org/wbillingsley/mavenrepo/raw/master/releases/",
        "handy snapshots" at "https://bitbucket.org/wbillingsley/mavenrepo/raw/master/snapshots/"
    ) 


## How to use it

The primary part of configuration is to set your `onAuth` method -- what you want your app to do when the OAuth process has completed.

Write the method just as if it were any other method on your controller. The method will be passed a `Ref[UserRecord]` containing data from the social service. (`UserRecord` just contains the received data. It doesn't need to match your application's user classes.)

    def onAuth(loginData:Ref[UserRecord]) = Action { 
      implicit request => 
    
      // Do your stuff here
      
    }


Then tell Handy-Play-OAuth about it:


    import com.wbillingsley.handy.playoauth.PlayAuth
    
    PlayAuth.onAuth = MyController.onAuth


Then set up your routes. You can just forward a subpath to them.

    ->  /oauth   handyplayoauth.Routes

This gives you paths:

* `/oauth/twitter` to start sign in with Twitter
* `/oauth/github` to start sign in with GitHub
* etc.


By default, the routes are `POST` routes, rather than `GET` routes. But if you want to enable `GET` routes as well, you can:

    PlayAuth.allowGet = true

And the last thing we need to do is set the client keys and secrets for the different services. There's two ways of doing this.

By default, Handy Play OAuth looks for configuration settings (which you can set in `application.conf`).  The settings it looks for are:

* auth.github.ckey
* auth.github.csecret
* auth.twitter.ckey
* auth.twitter.csecret
* *etc*

Or you can set them programmatically:

    import com.wbillingsley.handy.playoauth.controllers._

    GitHubController.clientKey = Some(myKey)
    GitHubController.clientSecret = Some(mySecret)
    TwitterController.clientKey = Some(myKey)
    TwitterController.clientSecret = Some(mySecret)

Congratulations, you're ready to go.

## Getting a list of configured services

You can get a list of configured services (those where you've set the client key and secret) by calling:

    PlayAuth.enabledServices



