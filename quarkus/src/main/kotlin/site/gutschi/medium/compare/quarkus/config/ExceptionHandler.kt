package site.gutschi.medium.compare.quarkus.config

import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.RedirectionException
import jakarta.ws.rs.ServerErrorException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.slf4j.LoggerFactory

@Provider
class ExceptionHandler : ExceptionMapper<Exception> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun toResponse(exception: Exception): Response {
        if (exception is RedirectionException) {
            logger.debug("Redirect to {}", exception.location)
            return exception.response
        }
        if (exception is NotFoundException) {
            logger.info("Resource not found {}", exception.message)
            return exception.response
        }
        if (exception is WebApplicationException) {
            logger.warn("Request failed with status {}", exception.response.status, exception)
            return exception.response
        }
        logger.error("Request failed with unexpected ", exception)
        val e2 = ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR, exception)
        return e2.response
    }
}