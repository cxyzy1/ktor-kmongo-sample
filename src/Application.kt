import com.fasterxml.jackson.databind.SerializationFeature
import com.mongodb.MongoClientException
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.jackson.jackson
import io.ktor.locations.Locations
import io.ktor.request.path
import io.ktor.routing.routing
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import org.slf4j.event.Level

lateinit var mongoClient: CoroutineClient

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(Locations) {
    }

    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(DataConversion)

    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
        header("X-Environment", "Dev")
    }

    routing {
        userRoutes()
    }

    val connectionString: String? = System.getenv("mongo.connectionString") ?: environment.config.propertyOrNull("mongo.connectionString")?.getString()
    connectionString?.let {
        mongoClient = KMongo.createClient(connectionString = it).coroutine
    } ?: throw MongoClientException("MongoDB connection string not defined")
}