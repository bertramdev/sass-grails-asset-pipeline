package asset.pipeline.sass
import asset.pipeline.AssetHelper
import org.mozilla.javascript.Context
import org.mozilla.javascript.JavaScriptException
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.NativeArray
import org.springframework.core.io.ClassPathResource
import groovy.util.logging.Log4j
import asset.pipeline.CacheManager
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

@Log4j
class SassProcessor {
  public static final java.lang.ThreadLocal threadLocal = new ThreadLocal();
  ScriptingContainer container
  ClassLoader classLoader
  def precompilerMode

  SassProcessor(precompiler=false){
    this.precompilerMode = precompiler
    try {
      container = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
      container.runScriptlet(buildInitializationScript())
      def workDir = new File("target/assets")
      if(!workDir.exists()) {
        workDir.mkdir()
      }
      container.put("to_path",workDir.canonicalPath)

    } catch (Exception e) {
      throw new Exception("SASS Engine initialization failed.", e)
    } finally {
      try {
        Context.exit()
      } catch (IllegalStateException e) {}
    }
  }

  private String buildInitializationScript() {
    StringWriter raw = new StringWriter();
    PrintWriter script = new PrintWriter(raw);

    script.println("require 'rubygems'                                                         ");
    script.println("require 'sass'                                                          ");
    script.println("require 'sass/plugin'                                                          ");
    script.println("require 'compass'                                                          ");
    script.println("frameworks = Dir.new(Compass::Frameworks::DEFAULT_FRAMEWORKS_PATH).path    ");
    script.println("Compass::Frameworks.register_directory(File.join(frameworks, 'compass'))   ");
    script.println("Compass::Frameworks.register_directory(File.join(frameworks, 'blueprint')) ");
    // script.println("Compass.add_project_configuration '" + getConfigLocation() + "'            ");
    script.println("Compass.configure_sass_plugin!                                             ");

    script.flush();

    return raw.toString();
  }



  def process(input, assetFile) {
      if(!this.precompilerMode) {
        threadLocal.set(assetFile);
      }
      def assetRelativePath = relativePath(assetFile.file)
      // def paths = AssetHelper.scopedDirectoryPaths(new File("grails-app/assets").getAbsolutePath())

      // paths += [assetFile.file.getParent()]
      def paths = AssetHelper.getAssetPaths()
      def relativePaths = paths.collect { [it,assetRelativePath].join(AssetHelper.DIRECTIVE_FILE_SEPARATOR)}
      // println paths
      paths = relativePaths + paths


      def pathstext = paths.collect{
        def p = it.replaceAll("\\\\", "/")
        if (p.endsWith("/")) {
          "${p}"
        } else {
          "${p}/"
        }
      }.join(",")
      container.put("assetFilePath", assetFile.file.canonicalPath)
      container.put("load_paths", pathstext)
      container.put("working_path", assetFile.file.getParent())
      // container.runScriptlet("Sass::Plugin.options[:load_paths] = load_paths")

      def outputFileName = "target/assets/${AssetHelper.fileNameWithoutExtensionFromArtefact(assetFile.file.name,assetFile)}.${assetFile.compiledExtension}".toString()
      container.put("file_dest", outputFileName)
      container.put("file_text", input)
      container.runScriptlet("""
        Compass.add_configuration(
            {
                :project_path => '.',
                :sass_path => working_path,
                :css_path => to_path,
                :additional_import_paths => load_paths.split(',')
            },
            'Grails' # A name for the configuration, can be anything you want
        )
      """)

      def configFile = new File(assetFile.file.getParent(), "config.rb")
      if(configFile.exists()) {
        container.put('config_file',configFile.canonicalPath)
        container.runScriptlet("""
          Compass.add_project_configuration config_file
        """)
      }

      container.runScriptlet("""
        Compass.configure_sass_plugin!
        Compass.compiler.compile(assetFilePath, file_dest)
      """)


      def outputFile = new File(outputFileName)
      if(outputFile.exists()) {
        if(assetFile.encoding) {
          return outputFile.getText(assetFile.encoding)
        }
        return outputFile.getText()
      } else {
        return input
      }


  }


  def relativePath(file, includeFileName=false) {
    def path
    if(includeFileName) {
      path = file.class.name == 'java.io.File' ? file.getCanonicalPath().split(AssetHelper.QUOTED_FILE_SEPARATOR) : file.file.getCanonicalPath().split(AssetHelper.QUOTED_FILE_SEPARATOR)
    } else {
      path = file.getParent().split(AssetHelper.QUOTED_FILE_SEPARATOR)
    }

    def startPosition = path.findLastIndexOf{ it == "grails-app" }
    if(startPosition == -1) {
      startPosition = path.findLastIndexOf{ it == 'web-app' }
      if(startPosition+2 >= path.length) {
        return ""
      }
      path = path[(startPosition+2)..-1]
    }
    else {
      if(startPosition+3 >= path.length) {
        return ""
      }
      path = path[(startPosition+3)..-1]
    }

    return path.join(AssetHelper.DIRECTIVE_FILE_SEPARATOR)
  }
}
