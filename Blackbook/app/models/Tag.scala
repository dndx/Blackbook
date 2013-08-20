package models

import anorm._
import anorm.SqlParser._

import play.api.db._
import play.api.Play.current

import scala.language.postfixOps

case class Tag(id: Long, name: String)

object Tag {

  /* Parses a tag from a SQL result set */
  val tag = { 
    get[Long]("Tags.Id") ~
    get[String]("Tags.Name") map { 
      case id ~ name => Tag(id, name)
    }
  }

  def normalizeName(name: String) = name.toUpperCase

  def all(): List[Tag] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM Tags").as(tag *)
  }

  def create(name: String) { 
    DB.withConnection { implicit c =>
      SQL("INSERT INTO Tags(Name) VALUES ({name})").on(
        'name -> normalizeName(name)).executeUpdate()
    }
  }

  def findTag(name: String): Option[Tag] = DB.withConnection { implicit c =>
    SQL("SELECT * FROM Tags WHERE Name = {name}").on(
      'name -> normalizeName(name)).as(tag *)
    match { 
      case found :: others => Some(found)
      case _ => None
    }
  }

  def findTag(id: Long): Option[Tag] = DB.withConnection { implicit c => 
    SQL("SELECT * FROM Tags WHERE Id = {id}").on(
      'id -> id).as(tag *)
    match { 
      case found :: others => Some(found)
      case _ => None
    }
  }

  def delete(id: Long) { 
    DB.withConnection { implicit c =>
      SQL("DELETE FROM Tags WHERE Id = {id}").on(
        'id -> id).executeUpdate()
    }
  }

  def getProducts(tagId: Long): List[Product] = DB.withConnection { implicit c =>
    SQL("""
      SELECT Products.Id, Products.Name FROM ProductTags 
        JOIN Products ON Products.Id = ProductTags.ProductId
        JOIN Tags ON Tags.Id = ProductTags.TagId
        WHERE Tags.Id = {tagId}
      """
      ).on('tagId -> tagId).as(Product.product *)
  }
}