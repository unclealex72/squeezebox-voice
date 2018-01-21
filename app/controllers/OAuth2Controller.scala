package controllers

import javax.inject.Inject

import akka.stream.Materializer
import auth._
import cats.implicits._
import monads._
import play.api.Logger
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scalaoauth2.provider.{OAuth2Provider, TokenEndpoint}
/**
  * Created by alex on 30/12/17
  **/
class OAuth2Controller @Inject()(
                                  cc: ControllerComponents,
                                  authenticationService: AuthenticationService,
                                  oauthClientDao: OauthClientDao,
                                  dataHandler: UserDataHandler,
                                  override val tokenEndpoint: TokenEndpoint,
                                  userDao: UserDao)(implicit ec: ExecutionContext, mat: Materializer) extends AbstractController(cc) with OAuth2Provider with play.api.i18n.I18nSupport {


  def accessToken: Action[AnyContent] = Action.async { implicit request =>
    issueAccessToken(dataHandler)
  }

  def schema = Action { implicit request =>
    val slickUserDao = userDao.asInstanceOf[SlickUserDao]
    val statements = slickUserDao.createStatements ++ slickUserDao.dropStatements
    Ok(statements.map(s => s + ";").mkString("\n"))
  }

  val loginForm: Form[LoginData] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText,
      "client_id" -> nonEmptyText,
      "state" -> default(text, "")
    )(LoginData.apply)(LoginData.unapply)
  )

  implicit class LoginFormExtensions(loginForm: Form[LoginData]) {

    def maybeClientId: Option[String] = loginForm.data.get("client_id")

    private def addFields(fields: Seq[(String, String)]): Form[LoginData] = {
      loginForm.copy(data = loginForm.data ++ fields.toMap)
    }

    def withClientId(oauthClient: OauthClient): Form[LoginData] = {
      addFields(Seq("client_id" -> oauthClient.clientId))
    }

    def withState(maybeState: Option[String]): Form[LoginData] = {
      addFields(maybeState.map(state => "state" -> state).toSeq)
    }

    def withNoSuchUser: Form[LoginData] = {
      loginForm.withGlobalError("no.such.user")
    }
  }

  def withOauthClient(maybeOauthClientId: Option[String])(block: OauthClient => Future[Result])
                     (implicit request: Request[AnyContent]): Future[Result] = {
    maybeOauthClientId match {
      case Some(clientId) =>
        oauthClientDao.findByClientId(clientId).value.flatMap {
          case Some(oauthClient) => block(oauthClient)
          case None => Future.successful(BadRequest(views.html.invalidClient(Some(clientId))))
        }
      case None => Future.successful(BadRequest(views.html.invalidClient(None)))
    }
  }

  def authStart = Action.async { implicit request =>
    def param(name: String): Option[String] = request.queryString.getOrElse(name, Seq.empty).headOption
    val maybeState = param("state")
    withOauthClient(param("client_id")) { oauthClient =>
      Future.successful {
        Ok(views.html.login(loginForm.withClientId(oauthClient).withState(maybeState), oauthClient))
      }
    }
  }

  def authProceed = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        withOauthClient(formWithErrors.maybeClientId) { oauthClient =>
          Future.successful {
            Ok(views.html.login(formWithErrors, oauthClient))
          }
        }
      },
      loginData => {
        withOauthClient(Some(loginData.clientId)) { oauthClient =>
          val meAuthorizationCode: FutureOption[OauthAuthorizationCode] = for {
            user <- authenticationService.findByCredentials(loginData.username, loginData.password)
            oauthAuthorizationCode <- authenticationService.createAuthorizationCode(user, oauthClient).^
          } yield {
            oauthAuthorizationCode
          }
          meAuthorizationCode.value.map {
            case Some(oauthAuthorizationCode) =>
              val parameters: Map[String, Seq[String]] = Map(
                "code" -> Seq(oauthAuthorizationCode.code),
                "state" -> Seq(loginData.state)
              )
              Redirect(oauthAuthorizationCode.oauthClient.redirectUri, parameters)
            case None =>
              BadRequest(views.html.login(loginForm.fill(loginData).withNoSuchUser, oauthClient))
          }
        }
      }
    )
  }
}

case class LoginData(
                      username: String,
                      password: String,
                      clientId: String,
                      state: String)

