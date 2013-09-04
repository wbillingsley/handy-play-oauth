package com.wbillingsley.handy.playoauth

import com.wbillingsley.handy.Ref
import play.api.mvc.Results
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.Action


object PlayAuth {
    
  /**
   * What the module should do on completion of an OAuth sign in
   */
  var onAuth: Ref[UserRecord] => play.api.mvc.EssentialAction = { rim =>
    import play.api.libs.concurrent.Execution.Implicits._
    
    val res = for (im <- rim) yield Results.Ok(UserRecord.jsonFormat.writes(im))
    val fr = for (opt <- res.toFuture) yield opt.getOrElse(Results.Forbidden("No authorisation structure came back"))
    
    Action { Results.Async(fr) }
    
  }  

}