import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import ratpack.groovy.template.TextTemplateModule
import ratpack.http.client.HttpClient

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  bindings {
    add TextTemplateModule
  }

  handlers {

      prefix("api") {

          /**
           * Retreive the details of a
           */
          get("ranks/:id") {
              def orgId = pathTokens.get("id")
              def client = get(HttpClient)

              client.get( new URI("https://api.github.com/orgs/${orgId}/repos"), { action ->
                  action.headers.add "User-Agent", "ratpack-http-client"
              }).then { response ->

                  def repos = new JsonSlurper().parse( response.body.inputStream ).sort{ -1 * it.watchers }

                  context.render JsonOutput.toJson( repos )
              }

          }

          get("org/:orgId/:repoId/commits") {

              def orgId  = pathTokens.get("orgId")
              def repoId = pathTokens.get("repoId")

              client.get( new URI("https://api.github.com/orgs/${orgId}"), { action ->
                  action.headers.add "User-Agent", "ratpack-http-client"
              }).then { response ->
                  context.render response.body.text
              }
          }

      }

    get {
      render groovyTemplate( "index.html", title: "GitHub Ranker" )
    }
        
    assets "public"
  }
}
