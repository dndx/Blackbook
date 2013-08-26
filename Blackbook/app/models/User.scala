package models

import anorm.SqlParser._
import anorm._
import controllers.Assets
import java.io.File
import java.util.Date
import play.api.Play.current
import play.api.db._
import scala.language.postfixOps
import scala.collection.immutable.BitSet
import util.Db

object Permission extends Enumeration {
  val ViewProducts = Value(1)
  val EditProducts = Value(2)
  val ViewLedger = Value(4)
  val EditLedger = Value(8)
  val ViewUsers = Value(16)
  val EditUsers = Value(32)
}

object User {
  type Permissions = Set[Permission.Value]

  var userMap: Map[String, User] = Map()

  trait User {
    def name(): String

    def hasPermission(perm: Permission.Value): Boolean
    def hasPermissions(perms: Permissions): Boolean =
      perms.forall(hasPermission)

    def authenticate(password: String): Boolean
  }

  class TestingAdmin extends User {
    def name() = "test"
    def hasPermission(perm: Permission.Value) = true
    def authenticate(password: String) = password == "secret"
  }

  case class DbUser
    ( id: Long,
      username: String,
      password: String,
      permissions: Long,
      enabled: Boolean
    ) extends User {
    def name() = "username"
    def hasPermission(perm: Permission.Value) = true
    def authenticate(password: String) = password == this.password
  }

  // Parses a DB user from a SQL result set.
  val dbUser = {
    get[Long]("Users.Id") ~
    get[String]("Users.Name") ~
    get[String]("Users.Password") ~
    get[Long]("Users.Permissions") ~
    get[Boolean]("Users.Enabled") map (flatten) map ((DbUser.apply _).tupled)
  }

  private def testUser(username: String): Option[User] = {
    if (username == "test") { Some(new TestingAdmin()) } else { None }
  }

  private def factory(username: String): Option[User] = {
    DB.withConnection { implicit c =>
      SQL("SELECT * FROM Users WHERE Name = {name}").
        on('name -> username).as(dbUser *)
    }.headOption orElse testUser(username)
  }

  def getUser(username: String): Option[User] = {
    userMap.get(username) orElse factory(username) map { user =>
      userMap += (username -> user)
      user
    }
  }

  def authenticate(username: String, password: String): Option[User] = {
    getUser(username) filter { 
      user => user.authenticate(password) 
    }
  }

}

