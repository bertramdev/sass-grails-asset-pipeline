grails.project.work.dir = 'target'

grails.project.dependency.resolution = {
    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        grailsPlugins()
        mavenCentral()
    }
    dependencies {
        compile 'org.jruby:jruby-complete:1.7.11'
    }

    plugins {
        runtime ":asset-pipeline:1.7.6"

        build(":release:2.2.1",
              ":rest-client-builder:1.0.3") {
            export = false
        }
    }
}
