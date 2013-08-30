package com.wbillingsley.handy.playoauth

import com.wbillingsley.handy.Ref
import play.api.mvc.Results
import play.api.libs.concurrent.Execution.Implicits.defaultContext


object PlayAuth {
  
  /**
   * What the module should do on completion of an OAuth sign in
   */
  var onAuth: (Ref[UserRecord]) => play.api.mvc.Result = { (rim) =>
    import play.api.libs.concurrent.Execution.Implicits._
    
    val res = for (im <- rim) yield UserRecord.jsonFormat.writes(im)
    Results.Async {
      for (
        opt <- res.toFuture        
      ) yield {
        opt.map(Results.Ok(_)).getOrElse(Results.Forbidden("No authorisation structure came back"))
      }
    }
    
  }

}