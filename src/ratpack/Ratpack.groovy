import groovyx.net.http.HTTPBuilder
import org.ajbrown.rankerizer.handler.GithubApiHandler
import ratpack.groovy.template.TextTemplateModule

import static ratpack.groovy.Groovy.groovyTemplate
import static ratpack.groovy.Groovy.ratpack

ratpack {
  bindings {
      add TextTemplateModule
      bind HTTPBuilder
      bind GithubApiHandler
  }

  handlers {


      prefix( "api", registry.get(GithubApiHandler) )

      get {
          render groovyTemplate( "index.html", title: "GitHub Ranker" )
      }
        
    assets "public"
  }
}
