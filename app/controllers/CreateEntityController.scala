package controllers

import javax.inject._

import auth.{AuthenticationService, RandomStringGenerator, UserDao}
import cats.data._
import cats.implicits._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class CreateEntityController @Inject()(
                                        cc: ControllerComponents,
                                        authenticationService: AuthenticationService,
                                        randomStringGenerator: RandomStringGenerator)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def nonEmptyString(body: JsValue)(fieldName: String): ValidatedNel[String, String] = {
    (body \ fieldName).asOpt[String].filterNot(_.isEmpty).toRight(s"$fieldName is required").toValidatedNel
  }
  case class ProtoUser(username: String, password: String)
  def createUser: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    val nonEmpty = nonEmptyString(request.body) _
    val validatedUsername: ValidatedNel[String, String] = nonEmpty("username")
    val validatedPassword: ValidatedNel[String, String] = nonEmpty("password")
    val validatedProtoUser: ValidatedNel[String, ProtoUser] = (validatedUsername, validatedPassword).mapN(ProtoUser(_, _))
    validatedProtoUser match {
      case Validated.Valid(protoUser) =>
        authenticationService.createUser(protoUser.username, protoUser.password).map { user =>
          val response = Json.obj("id" -> JsNumber(user.id), "username" -> JsString(user.username))
          Created(response)
        }
      case Validated.Invalid(errs) =>
        val response = Json.obj("errors" -> JsArray(errs.toList.map(err => JsString(err))))
        Future.successful(BadRequest(response))
    }
  }

  case class ProtoClient(owner: String, clientId: String, clientSecret: String, maybeScope: Option[String], redirectUri: String)
  def createClient: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    val nonEmpty = nonEmptyString(request.body) _
    val validatedOwner: ValidatedNel[String, String] = nonEmpty("owner")
    val validatedClientId: ValidatedNel[String, String] = nonEmpty("clientId")
    val validatedRedirectUri: ValidatedNel[String, String] = nonEmpty("redirectUri")
    val clientSecret = randomStringGenerator.generate
    val maybeScope = (request.body \ "scope").asOpt[String]
    val validatedClient: ValidatedNel[String, ProtoClient] =
      (validatedOwner, validatedClientId, validatedRedirectUri).mapN(ProtoClient(_, _, clientSecret, maybeScope, _))
    validatedClient match {
      case Validated.Valid(protoClient) =>
        authenticationService.createClient(
          protoClient.owner,
          protoClient.clientId,
          protoClient.clientSecret,
          protoClient.maybeScope,
          protoClient.redirectUri).map { client =>
          val fields = Map("id" -> JsNumber(client.id),
            "owner" -> JsString(client.owner.username),
            "clientId" -> JsString(client.clientId),
            "clientSecret" -> JsString(client.clientSecret),
            "redirectUri" -> JsString(client.redirectUri)
          ) ++ client.scope.map(scope => "scope" -> JsString(scope))
          val response = JsObject(fields)
          Created(response)
        }.getOrElse(NotFound(Json.obj("owner" -> protoClient.owner)))
      case Validated.Invalid(errs) =>
        val response = Json.obj("errors" -> JsArray(errs.toList.map(err => JsString(err))))
        Future.successful(BadRequest(response))
    }
  }

}
