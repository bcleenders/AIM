name := "Analysis"

version := "1.0"

scalaVersion := "2.11.6"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "org.apache.spark"      %% "spark-core"           % "1.3.0" ,
  "org.apache.spark"      %% "spark-mllib"          % "1.3.0",
  "edu.arizona.sista"     %% "processors"           % "5.2",
  "edu.arizona.sista"     %% "processors"           % "5.2" classifier "models",
  "com.github.rholder"    % "snowball-stemmer"      % "1.3.0.581.1",
  "com.github.mauricio"   %% "postgresql-async"     % "0.2.15",
  "org.json4s"            %% "json4s-jackson"       % "3.2.11"
)
    