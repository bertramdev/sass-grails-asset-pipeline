import asset.pipeline.AssetHelper
import asset.pipeline.sass.SassAssetFile

class SassAssetPipelineGrailsPlugin {
    def version         = "1.9.2"
    def grailsVersion   = "2.0 > *"
    def title           = "SASS/SCSS Asset-Pipeline Plugin"
    def author          = "David Estes"
    def authorEmail     = "destes@bcap.com"
    def description     = "Provides SASS/SCSS Compass support for the asset-pipeline static asset management plugin."
    def documentation   = "http://github.com/bertramdev/sass-grails-asset-pipeline"
    def license         = "APACHE"
    def organization    = [ name: "Bertram Capital", url: "http://www.bertramcapital.com/" ]
    def issueManagement = [ system: "GITHUB", url: "http://github.com/bertramdev/sass-grails-asset-pipeline/issues" ]
    def scm             = [ url: "http://github.com/bertramdev/sass-grails-asset-pipeline" ]
    def developers      = [ [name: 'Brian Wheeler'], [name: 'Jeremy Leng'], [name: 'Jordon Saardchit'], [name: 'Jeremy Crosbie'], [name: 'Bob Whiton'], [name: 'Andy Warner'] ]

    def doWithDynamicMethods = { ctx ->
        AssetHelper.assetSpecs << SassAssetFile
    }
}
