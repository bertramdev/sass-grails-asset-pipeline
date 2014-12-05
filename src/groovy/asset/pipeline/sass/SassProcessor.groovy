/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package asset.pipeline.sass

import asset.pipeline.AssetHelper
import grails.util.Holders
import org.mozilla.javascript.Context
import org.mozilla.javascript.JavaScriptException
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.NativeArray
import org.springframework.core.io.ClassPathResource
import groovy.util.logging.Log4j
import asset.pipeline.CacheManager
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import asset.pipeline.AbstractProcessor
import asset.pipeline.AssetCompiler

@Log4j
class SassProcessor extends AbstractProcessor {
    public static final java.lang.ThreadLocal threadLocal = new ThreadLocal();
    private static final $LOCK = new Object[0]
    static ScriptingContainer container
    ClassLoader classLoader


    SassProcessor(AssetCompiler precompiler) {
        super(precompiler)
        try {
            synchronized($LOCK) {
                if(!SassProcessor.container) {
                    SassProcessor.container = new ScriptingContainer(LocalVariableBehavior.PERSISTENT);
                    SassProcessor.container.setEnvironment([:])    
                    SassProcessor.container.runScriptlet(buildInitializationScript())
                    overrideSassFileImporter()
                }

                loadPluginContextPaths()
            }
        } catch (Exception e) {
            throw new Exception("SASS Engine initialization failed.", e)
        }
    }

    private String buildInitializationScript() {
        """
        if !defined?(Compass)
          require 'rubygems'
          require 'java'
          require 'sass'
          require 'sass/plugin'
          require 'compass'
        end
        
        frameworks = Dir.new(Compass::Frameworks::DEFAULT_FRAMEWORKS_PATH).path
        Compass::Frameworks.register_directory(File.join(frameworks,'compass'))
        Compass::Frameworks.register_directory(File.join(frameworks,'blueprint'))
        Compass.configure_sass_plugin!
        """
    }

    private overrideSassFileImporter() {
        container.runScriptlet("""
            Sass::Importers::Filesystem.class_eval do

                def find(name,options)
                    result = _find(@root, name, options)
                    if result
                        Java::AssetPipelineSass::SassProcessor.onImport(result.options[:filename])
                    end
                    return result
                end
                def find_relative(name, base, options)
                  result = _find(File.dirname(base), name, options)
                  if result
                      Java::AssetPipelineSass::SassProcessor.onImport(result.options[:filename])
                  end
                  return result
                end
            end
        """);
    }

    private loadPluginContextPaths() {
        container.runScriptlet("PLUGIN_CONTEXT_PATHS = {}  if !defined?(PLUGIN_CONTEXT_PATHS)")
        for(plugin in GrailsPluginUtils.pluginInfos) {
            def pluginContextPath = plugin.pluginDir.getPath()
            container.put("plugin_context", pluginContextPath)
            container.put("plugin_name", plugin.name)
            container.runScriptlet("PLUGIN_CONTEXT_PATHS[plugin_name] = plugin_context")
        }
    }

    def process(input, assetFile) {

        def grailsApplication = Holders.getGrailsApplication()

        if(!this.precompiler) {
            threadLocal.set(assetFile);
        }
        def assetRelativePath = relativePath(assetFile.file)
        def fileText
        def workDir = new File("target/assets", assetRelativePath)
        if(!workDir.exists()) {
            workDir.mkdirs()
        }
        synchronized($LOCK) {
            container.put("to_path",workDir.canonicalPath)

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

            def outputStyle = ":${grailsApplication.config?.grails?.assets?.minifyCss ? 'compressed' : 'expanded'}"

            def additionalFiles = []
            container.put("asset_relative_path", assetRelativePath)
            container.put("assetFilePath", assetFile.file.canonicalPath.replace(File.separator,AssetHelper.DIRECTIVE_FILE_SEPARATOR))
            container.put("load_paths", pathstext)
            container.put("project_path", new File('.').canonicalPath.replace(File.separator,AssetHelper.DIRECTIVE_FILE_SEPARATOR))
            container.put("working_path", assetFile.file.getParent().replace(File.separator,AssetHelper.DIRECTIVE_FILE_SEPARATOR))
            container.put("asset_path", assetBasePath(assetFile.file))
            container.put("precompiler_mode",precompiler ? true : false)
            container.put("additional_files", additionalFiles)
            def outputFileName = new File(assetFile.file.getParent(),"${AssetHelper.fileNameWithoutExtensionFromArtefact(assetFile.file.name,assetFile)}.${assetFile.compiledExtension}".toString()).canonicalPath.replace(File.separator,AssetHelper.DIRECTIVE_FILE_SEPARATOR)
            try {
                container.put("file_dest", outputFileName)
                container.runScriptlet("""
                    Compass.reset_configuration!
                    environment = precompiler_mode ? :production : :development

                    Compass.add_configuration(
                    {
                    :cache_path   => project_path + '/.sass-cache',
                    :cache => true,
                    :project_path => working_path,
                    :environment =>  environment,
                    :images_path  => asset_path + '/images',
                    :fonts_path   => asset_path + '/fonts',
                    :generated_images_path => asset_path + '/images',
                    :relative_assets => true,
                    :sass_path => working_path,
                    :css_path => working_path,
                    :additional_import_paths => load_paths.split(','),
                    :output_style => ${outputStyle}
                    },
                    'Grails' # A name for the configuration, can be anything you want
                    )

                    Compass.configuration.on_sprite_saved do |filename|
                        pathname = Pathname.new(filename)
                        additional_files << pathname.cleanpath.to_s
                    end

                """)

                def configFile = new File(assetFile.file.getParent(), "config.rb")
                if(configFile.exists()) {
                    container.put('config_file',configFile.canonicalPath.replace(File.separator,AssetHelper.DIRECTIVE_FILE_SEPARATOR))
                } else {
                    container.put('config_file',null)
                }


                container.runScriptlet("""
                Dir.chdir(working_path) do
                    Compass.configure_sass_plugin!
                    Compass.add_project_configuration config_file if config_file
                    Compass.compiler.compile_if_required(assetFilePath, file_dest)
                end
                """)

                // Lets check for generated files and add to precompiler
                if(precompiler) {
                    additionalFiles.each { filename ->
                        def file = new File(filename)
                        precompiler.filesToProcess << relativePath(file,true)
                    }
                }

                def outputFile = new File(outputFileName)
                if(outputFile.exists()) {
                    if(assetFile.encoding) {
                        fileText = outputFile.getText(assetFile.encoding)
                    } else {
                        fileText = outputFile.getText()
                    }
                } else {
                    fileText = input
                }
            } catch(e) {
                throw(e)
            } finally {
                def outputFile = new File(outputFileName)
                if(outputFile.exists()) {
                    outputFile.delete()
                }
            }
        }
        return fileText
    }

    // Return the parent asset path for the file
    def assetBasePath(file) {
        def path
        path  = file.canonicalPath.split(AssetHelper.QUOTED_FILE_SEPARATOR)

        def endPosition = path.findLastIndexOf {it == "assets" || it == "web-app"}
        if(endPosition == -1) {
            return new File("grails-app/assets").canonicalPath.replace(File.separator, AssetHelper.DIRECTIVE_FILE_SEPARATOR)
        } else {
            path = path[0..endPosition]
            return path.join(AssetHelper.DIRECTIVE_FILE_SEPARATOR)
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
        } else {
            if(startPosition+3 >= path.length) {
               return ""
            }
            path = path[(startPosition+3)..-1]
        }

        return path.join(AssetHelper.DIRECTIVE_FILE_SEPARATOR)
    }

    static String onImport(String path) {
        def assetFile = threadLocal.get();
        def file = new File(path) //Returned from the Sass File Importer
        if(assetFile) {
          CacheManager.addCacheDependency(assetFile.file.canonicalPath, file)
        }

        return null
    }
}
