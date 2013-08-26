package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import play.api.Play.current
import java.io.File
import scala.collection.mutable.ArrayBuffer

object Products extends Controller {

  val productForm = Form (
    tuple("name" -> nonEmptyText,
          "description" -> nonEmptyText)
  )
  
  def products = Action {
    Ok(views.html.products.index(Product.all(), productForm))
  }
  
  def viewProduct(id: Long) = Action {
    val product = Product.find(id)
    product match { 
      case Some(p) => Ok(views.html.products.view(p))
      case None => BadRequest(views.html.products.index(Product.all(), productForm))
    }
  }
  
  def editProduct(id: Long) = Action {
    val product = Product.find(id)
    product match { 
      case Some(p) => Ok(views.html.products.edit(p,productForm.fill((p.name, p.description))))
      case None => throw new Exception("No product " + id + " found.")
    }
  }
  
  def updateProduct(id: Long) = Action { implicit request =>
    productForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.products.edit(Product.find(id).get, formWithErrors)),
      form => {
        val (name, description) = form
        val product = Product.find(id)
	    product match { 
	      case Some(p) => {Product.update(p.id, name, description); Redirect(routes.Products.editProduct(p.id))}
	      case None => BadRequest(views.html.products.index(Product.all(), productForm))
	    }
      })
  }

  def newProduct() = Action {
    Ok(views.html.products.newProduct(productForm))
  }
  
  def createProduct = Action { implicit request =>
    productForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.products.newProduct(formWithErrors)),
      form => {
        val (name, description) = form
        val product = Product.create(name, description)
        Redirect(routes.Products.editProduct(product.id))
      })
  }

  def deleteProduct(id: Long) = Action {
    Product.delete(id)
    Redirect(routes.Products.products)
  }

  def tagsForProduct(id: Long) = Action { 
    val product = Product.find(id)

    product match { 
      case Some(p) => Ok(views.html.product_tags(p))
      case None => BadRequest(views.html.products.index(Product.all(), productForm))
    }
  }
  
  private[this] def getProductFilePath(id: Long, filename:String):File = {
    return new File("/tmp/products/" + id + "/files/" + filename)
  }
  
  // This should be used only for determining where to save an icon 
  // file when one is upload, not for getting the icon file
  private[this] def getProductIconPath(id: Long):File = {
    return new File("/tmp/products/" + id + "/icon.png")
  }
  
  // This should be used for getting the icon file, 
  // this includes a default icon file if an icon is not found
  private[this] def getProductIconFile(id: Long):File = {
    var file = getProductIconPath(id)
    if(file.exists()){
      return file;
    } else {
      return Play.getFile("public/images/default-product-icon.png")
    }
  }
  
  def getIcon(id: Long) = Action {
    Ok.sendFile(getProductIconFile(id))
  }
  
  def getFile(id: Long, filename:String) = Action {
    Ok.sendFile(getProductFilePath(id, filename))
  }
  
  def deleteFile(id: Long, filename:String) = Action {
    getProductFilePath(id, filename).delete()
    Redirect(routes.Products.editProduct(id))
  }
  
  def uploadProductFile(id: Long) = Action(parse.multipartFormData) { request =>
	  request.body.file("fileUpload").map { fileUpload =>
	    val filename = fileUpload.filename 
	    val contentType = fileUpload.contentType
	    fileUpload.ref.moveTo(getProductFilePath(id,filename), replace=true)
	    Redirect(routes.Products.editProduct(id))
	  }.getOrElse {
	    Redirect(routes.Products.editProduct(id)).flashing("error" -> "Missing file")
	  }
  }
  
  def uploadProductIcon(id: Long) = Action(parse.multipartFormData) { request =>
	  request.body.file("iconUpload").map { fileUpload =>
	    val contentType = fileUpload.contentType
	    if(contentType.get.toString().equals("image/png")){
	      fileUpload.ref.moveTo(getProductIconPath(id), replace=true)
	      Redirect(routes.Products.editProduct(id))
	    } else {
	      getProductIconPath(id).delete();
	      Redirect(routes.Products.editProduct(id)).flashing("error" -> (contentType.get.toString() + " is not a PNG file"))
	    }
	  }.getOrElse {
	    Redirect(routes.Products.editProduct(id)).flashing("error" -> "Missing file")
	  }
  }
}
