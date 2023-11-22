@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  omero-ms-pixel-buffer startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and OMERO_MS_PIXEL_BUFFER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\omero-ms-pixel-buffer-0.6.3.jar;%APP_HOME%\lib\simpleclient_vertx-0.6.0.jar;%APP_HOME%\lib\omero-ms-core-0.7.0.jar;%APP_HOME%\lib\brave-instrumentation-http-5.6.8.jar;%APP_HOME%\lib\brave-5.6.8.jar;%APP_HOME%\lib\brave-http-4.13.6.jar;%APP_HOME%\lib\zipkin-sender-okhttp3-2.10.0.jar;%APP_HOME%\lib\omero-blitz-5.6.2.jar;%APP_HOME%\lib\omero-server-5.6.7.jar;%APP_HOME%\lib\omero-renderer-5.5.12.jar;%APP_HOME%\lib\omero-romio-5.7.2.jar;%APP_HOME%\lib\omero-common-5.6.1.jar;%APP_HOME%\lib\metrics-logback-3.0.2.jar;%APP_HOME%\lib\logback-classic-1.2.11.jar;%APP_HOME%\lib\log4j-over-slf4j-1.7.32.jar;%APP_HOME%\lib\vertx-web-3.8.1.jar;%APP_HOME%\lib\vertx-config-yaml-3.8.1.jar;%APP_HOME%\lib\vertx-config-3.8.1.jar;%APP_HOME%\lib\collector-0.12.0.jar;%APP_HOME%\lib\simpleclient_hotspot-0.8.0.jar;%APP_HOME%\lib\icegrid-3.6.5.jar;%APP_HOME%\lib\simpleclient_common-0.6.0.jar;%APP_HOME%\lib\simpleclient-0.8.0.jar;%APP_HOME%\lib\brave-core-4.13.6.jar;%APP_HOME%\lib\zipkin-reporter-2.10.0.jar;%APP_HOME%\lib\zipkin-2.15.0.jar;%APP_HOME%\lib\omero-model-5.6.10.jar;%APP_HOME%\lib\formats-gpl-6.12.0.jar;%APP_HOME%\lib\formats-bsd-6.12.0.jar;%APP_HOME%\lib\formats-api-6.12.0.jar;%APP_HOME%\lib\ome-xml-6.3.2.jar;%APP_HOME%\lib\metakit-5.3.5.jar;%APP_HOME%\lib\ome-poi-5.3.7.jar;%APP_HOME%\lib\ome-codecs-0.4.4.jar;%APP_HOME%\lib\ome-common-6.0.14.jar;%APP_HOME%\lib\minio-5.0.2.jar;%APP_HOME%\lib\okhttp-3.14.2.jar;%APP_HOME%\lib\logback-core-1.2.11.jar;%APP_HOME%\lib\calcite-core-1.20.0.jar;%APP_HOME%\lib\spring-security-ldap-4.2.4.RELEASE.jar;%APP_HOME%\lib\spring-ldap-core-2.3.2.RELEASE.jar;%APP_HOME%\lib\avatica-core-1.15.0.jar;%APP_HOME%\lib\json-path-2.4.0.jar;%APP_HOME%\lib\avatica-metrics-1.15.0.jar;%APP_HOME%\lib\metrics-graphite-3.0.2.jar;%APP_HOME%\lib\metrics-jvm-3.0.2.jar;%APP_HOME%\lib\metrics-core-3.0.2.jar;%APP_HOME%\lib\hibernate-search-3.4.2.Final.jar;%APP_HOME%\lib\hibernate-core-3.6.10.Final.jar;%APP_HOME%\lib\bufr-3.0.jar;%APP_HOME%\lib\hibernate-commons-annotations-3.2.0.Final.jar;%APP_HOME%\lib\hibernate-search-analyzers-3.4.2.Final.jar;%APP_HOME%\lib\cdm-core-5.3.3.jar;%APP_HOME%\lib\jxrlib-all-0.2.4.jar;%APP_HOME%\lib\native-lib-loader-2.1.4.jar;%APP_HOME%\lib\httpservices-5.3.3.jar;%APP_HOME%\lib\solr-analysis-extras-3.1.0.jar;%APP_HOME%\lib\solr-core-3.1.0.jar;%APP_HOME%\lib\solr-solrj-3.1.0.jar;%APP_HOME%\lib\slf4j-api-1.7.32.jar;%APP_HOME%\lib\lettuce-core-5.2.0.RELEASE.jar;%APP_HOME%\lib\vertx-jdbc-client-3.8.1.jar;%APP_HOME%\lib\kaitai-struct-runtime-0.8.jar;%APP_HOME%\lib\aggdesigner-algorithm-6.0.jar;%APP_HOME%\lib\commons-lang-2.6.jar;%APP_HOME%\lib\vertx-web-common-3.8.1.jar;%APP_HOME%\lib\vertx-auth-common-3.8.1.jar;%APP_HOME%\lib\vertx-bridge-common-3.8.1.jar;%APP_HOME%\lib\vertx-sql-common-3.8.1.jar;%APP_HOME%\lib\vertx-core-3.8.1.jar;%APP_HOME%\lib\jackson-databind-2.14.1.jar;%APP_HOME%\lib\jackson-annotations-2.14.1.jar;%APP_HOME%\lib\esri-geometry-api-2.2.0.jar;%APP_HOME%\lib\jackson-core-2.14.1.jar;%APP_HOME%\lib\jackson-dataformat-yaml-2.14.1.jar;%APP_HOME%\lib\javax.activation-1.2.0.jar;%APP_HOME%\lib\snakeyaml-1.33.jar;%APP_HOME%\lib\okio-1.17.2.jar;%APP_HOME%\lib\glacier2-3.6.5.jar;%APP_HOME%\lib\ice-3.6.5.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.39.Final.jar;%APP_HOME%\lib\netty-codec-http2-4.1.39.Final.jar;%APP_HOME%\lib\netty-codec-http-4.1.39.Final.jar;%APP_HOME%\lib\netty-handler-4.1.42.Final.jar;%APP_HOME%\lib\netty-resolver-dns-4.1.39.Final.jar;%APP_HOME%\lib\netty-codec-socks-4.1.39.Final.jar;%APP_HOME%\lib\netty-codec-dns-4.1.39.Final.jar;%APP_HOME%\lib\netty-codec-4.1.42.Final.jar;%APP_HOME%\lib\netty-transport-4.1.42.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.42.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.42.Final.jar;%APP_HOME%\lib\netty-common-4.1.42.Final.jar;%APP_HOME%\lib\reactor-core-3.3.0.RELEASE.jar;%APP_HOME%\lib\c3p0-0.9.5.4.jar;%APP_HOME%\lib\spring-context-support-4.3.22.RELEASE.jar;%APP_HOME%\lib\spring-jms-4.3.14.RELEASE.jar;%APP_HOME%\lib\postgresql-42.2.1.jar;%APP_HOME%\lib\btm-3.0.0-mk1.jar;%APP_HOME%\lib\javax.mail-1.6.2.jar;%APP_HOME%\lib\jhdf5-19.04.0.jar;%APP_HOME%\lib\base-18.09.0.jar;%APP_HOME%\lib\commons-io-2.7.jar;%APP_HOME%\lib\reactive-streams-1.0.3.jar;%APP_HOME%\lib\mchange-commons-java-0.2.15.jar;%APP_HOME%\lib\spring-messaging-4.3.14.RELEASE.jar;%APP_HOME%\lib\spring-orm-4.3.14.RELEASE.jar;%APP_HOME%\lib\spring-jdbc-4.3.14.RELEASE.jar;%APP_HOME%\lib\spring-tx-4.3.14.RELEASE.jar;%APP_HOME%\lib\spring-security-core-4.2.4.RELEASE.jar;%APP_HOME%\lib\spring-context-4.3.22.RELEASE.jar;%APP_HOME%\lib\spring-aop-4.3.22.RELEASE.jar;%APP_HOME%\lib\spring-beans-4.3.22.RELEASE.jar;%APP_HOME%\lib\spring-expression-4.3.22.RELEASE.jar;%APP_HOME%\lib\spring-core-4.3.22.RELEASE.jar;%APP_HOME%\lib\calcite-linq4j-1.20.0.jar;%APP_HOME%\lib\guava-29.0-jre.jar;%APP_HOME%\lib\sketches-core-0.9.0.jar;%APP_HOME%\lib\janino-3.0.11.jar;%APP_HOME%\lib\commons-compiler-3.0.11.jar;%APP_HOME%\lib\activation-1.1.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\aopalliance-1.0.jar;%APP_HOME%\lib\protobuf-java-3.6.1.jar;%APP_HOME%\lib\memory-0.9.0.jar;%APP_HOME%\lib\udunits-4.5.5.jar;%APP_HOME%\lib\antlr-2.7.6.jar;%APP_HOME%\lib\commons-collections-3.1.jar;%APP_HOME%\lib\dom4j-1.6.1.jar;%APP_HOME%\lib\hibernate-jpa-2.0-api-1.0.1.Final.jar;%APP_HOME%\lib\jta-1.1.jar;%APP_HOME%\lib\lucene-smartcn-3.1.0.jar;%APP_HOME%\lib\lucene-stempel-3.1.0.jar;%APP_HOME%\lib\lucene-misc-3.1.0.jar;%APP_HOME%\lib\lucene-spellchecker-3.1.0.jar;%APP_HOME%\lib\lucene-analyzers-3.1.0.jar;%APP_HOME%\lib\lucene-highlighter-3.1.0.jar;%APP_HOME%\lib\lucene-memory-3.1.0.jar;%APP_HOME%\lib\lucene-spatial-3.1.0.jar;%APP_HOME%\lib\lucene-core-3.1.0.jar;%APP_HOME%\lib\ome-mdbtools-5.3.2.jar;%APP_HOME%\lib\JWlz-1.4.0.jar;%APP_HOME%\lib\joda-time-2.10.3.jar;%APP_HOME%\lib\kryo-4.0.2.jar;%APP_HOME%\lib\json-20090211.jar;%APP_HOME%\lib\sqlite-jdbc-3.28.0.jar;%APP_HOME%\lib\jcip-annotations-1.0.jar;%APP_HOME%\lib\specification-6.3.2.jar;%APP_HOME%\lib\turbojpeg-6.12.0.jar;%APP_HOME%\lib\jgoodies-forms-1.7.2.jar;%APP_HOME%\lib\perf4j-0.9.16.jar;%APP_HOME%\lib\xml-apis-1.4.01.jar;%APP_HOME%\lib\re2j-1.3.jar;%APP_HOME%\lib\reflectasm-1.11.3.jar;%APP_HOME%\lib\minlog-1.3.0.jar;%APP_HOME%\lib\objenesis-2.5.1.jar;%APP_HOME%\lib\failureaccess-1.0.1.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\checker-qual-2.11.1.jar;%APP_HOME%\lib\error_prone_annotations-2.3.4.jar;%APP_HOME%\lib\j2objc-annotations-1.3.jar;%APP_HOME%\lib\google-http-client-xml-1.20.0.jar;%APP_HOME%\lib\ome-jai-0.1.3.jar;%APP_HOME%\lib\aircompressor-0.21.jar;%APP_HOME%\lib\jgoodies-common-1.7.0.jar;%APP_HOME%\lib\asm-5.0.4.jar;%APP_HOME%\lib\solr-commons-csv-3.1.0.jar;%APP_HOME%\lib\google-http-client-1.20.0.jar;%APP_HOME%\lib\xpp3-1.1.4c.jar


@rem Execute omero-ms-pixel-buffer
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %OMERO_MS_PIXEL_BUFFER_OPTS%  -classpath "%CLASSPATH%" io.vertx.core.Launcher %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable OMERO_MS_PIXEL_BUFFER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%OMERO_MS_PIXEL_BUFFER_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
