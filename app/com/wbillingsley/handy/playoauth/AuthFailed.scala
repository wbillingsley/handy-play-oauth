package com.wbillingsley.handy.playoauth

case class AuthFailed(msg:String) extends Exception(msg)

object AuthFailed {
  
  val DECLINED = AuthFailed("No user was authorized")
  
}