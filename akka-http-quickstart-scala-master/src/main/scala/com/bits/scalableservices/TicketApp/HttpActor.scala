package com.bits.scalableservices.TicketApp

import akka.actor.Actor
import com.bits.scalableservices.TicketApp.HttpActor.getCurrentUser
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.nio.client.{CloseableHttpAsyncClient, HttpAsyncClients}

import java.io.FileInputStream
import java.security.{KeyStore, SecureRandom}
import java.security.cert.X509Certificate
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory, X509TrustManager}
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.concurrent.FutureCallback

import scala.util.Try

object HttpActor{
  case class getCurrentUser()
}

class HttpActor extends Actor {
  val url = "http://localhost:8081/hello"
  override def receive: Receive = {
    case getCurrentUser() => {
      val httpGet = new HttpGet(url)
      val httpsClient = getByPassedClient("","", "false")
      try {
        httpsClient.start()
        val response = httpsClient.execute(httpGet, new FutureCallback[HttpResponse] {
          override def cancelled(): Unit = println("Inside HTTPS client Post Request got Cancelled")
          override def completed(result: HttpResponse): Unit = {
          }
          override def failed(ex: Exception): Unit = {
            println(ex.getMessage)
          }
        })
        val statusCode = response.get().getStatusLine.getStatusCode
        val inputStream = response.get().getEntity.getContent
        val userDetails = Try(scala.io.Source.fromInputStream(inputStream).getLines.mkString).getOrElse("")
        println(s"Content retrieved from the url: ${url} is ${userDetails}")
        sender ! userDetails
      }
      catch{
        case e: Exception =>
        {
          println(e.getMessage+"Failed")
          sender ! "Failed"
        }

      }
  }
  }

  def getByPassedClient(keyStoreFileName: String, jksPassword: String, certificate: String): CloseableHttpAsyncClient = {
    //logger.Info("Warning ! Https connections will be done without checking certificate. Do not use in production.")
    val httpClientBuilder = HttpAsyncClients.custom()
    if (certificate.equalsIgnoreCase("true")) {
      // import keystore
      val keyStorePassword = jksPassword // the password you used whit the command keytool
      val ks = KeyStore.getInstance(KeyStore.getDefaultType)
      val keyStorePath = getClass.getClassLoader.getResource(keyStoreFileName)

      val inputStream = new FileInputStream(keyStoreFileName) //keyStorePath.getPath)
      ks.load(inputStream, keyStorePassword.toArray)
      IOUtils.closeQuietly(inputStream)
      // create trust manager from keystore
      val tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
      tmf.init(ks)
      val trustManager = tmf.getTrustManagers
      // associate trust manager with the httpClient
      val sslContext = SSLContext.getInstance("SSL")
      sslContext.init(Array(), trustManager, null)
      httpClientBuilder.setSSLContext(sslContext)
    } else {
      val sslContext = SSLContext.getInstance("TLSv1.2")
      sslContext.init(null, Array(new X509TrustManager {
        override def getAcceptedIssuers: Array[X509Certificate] = Array.empty[X509Certificate]
        override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
        override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
      }), new SecureRandom())
      httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .setSSLContext(sslContext)
    }
    httpClientBuilder.build()
  }
}
