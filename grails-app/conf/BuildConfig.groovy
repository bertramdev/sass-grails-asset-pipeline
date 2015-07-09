grails.project.work.dir = 'target'

grails.project.dependency.resolution = {
    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        grailsPlugins()
        mavenCentral()
        jcenter()
    }
    dependencies {
        compile 'com.bertramlabs.plugins:sass-asset-pipeline:2.3.1'
    }

    plugins {
        runtime ":asset-pipeline:2.3.9"

        build(":release:3.0.1",
              ":rest-client-builder:2.0.3") {
            export = false
        }
    }
}
