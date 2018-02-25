package modules

import java.io.FileInputStream

import com.google.inject.Provider
import javax.inject.{Inject, Singleton}
import lexical.RemovePunctuationService
import play.api.Configuration
import play.api.inject.{SimpleModule, _}
import play.api.libs.json.Reads._
import play.api.libs.json._
import squeezebox.{RoomsProvider, StaticRoomsProvider}

/**
  * Created by alex on 26/12/17
  *
  **/
class RoomsModule extends SimpleModule(
  bind[RoomsProvider].toProvider[ConfigurationRoomsProvider]
)

@Singleton
class ConfigurationRoomsProvider @Inject() (config: Configuration, unpunctuated: RemovePunctuationService) extends Provider[RoomsProvider] {
  override def get(): RoomsProvider = {
    val roomsFile: String = config.get[String]("rooms.path")
    val in = new FileInputStream(roomsFile)
    val roomNamesById: Map[String, String] = try {
        Json.parse(in).as[Map[String, String]]
    }
    finally {
      in.close()
    }
    new StaticRoomsProvider(roomNamesById, unpunctuated)
  }
}
