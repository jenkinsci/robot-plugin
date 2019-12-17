# Changelog
#### Release 2.0.0

-   Pull request 18 [New design for list view results
    column](https://github.com/jenkinsci/robot-plugin/pull/18)
-   Pull request 22 [Support for \`rebot\` merged output.xml
    file](https://github.com/jenkinsci/robot-plugin/pull/22)
-   Pull request 23 [Add pipeline step
    support](https://github.com/jenkinsci/robot-plugin/pull/23)
-   Pull request 24 [Fix empty result in REST
    API](https://github.com/jenkinsci/robot-plugin/pull/24)
-   Pull request 25 [Add suite and test descriptions to results
    view](https://github.com/jenkinsci/robot-plugin/pull/25)

#### Release 1.6.5

-   Pull request 15 [Fix for evaluating case as critically
    failed](https://github.com/jenkinsci/robot-plugin/pull/15)
-   Pull request 16 [Removes second reading of entire history in
    createDurationGraphForTestObject](https://github.com/jenkinsci/robot-plugin/pull/16)
-   Pull request 19 [Changed new Robot
    Icons](https://github.com/jenkinsci/robot-plugin/pull/19)
-   Pull request 20 [\[FIXED JENKINS-47227\] Add public
    getEnableCache](https://github.com/jenkinsci/robot-plugin/pull/20)
-   Pull request 21 [Add
    Jenkinsfile](https://github.com/jenkinsci/robot-plugin/pull/21)
-   JENKINS-52447 [Incorrect reporting of total test
    cases](https://issues.jenkins-ci.org/browse/JENKINS-52447)

#### Release 1.6.4

-   Pull request 12 [Support Jenkins
    Workflow](https://github.com/jenkinsci/robot-plugin/pull/12)
-   Pull request 13 [Make cache based on WeakReferences
    optional](https://github.com/jenkinsci/robot-plugin/pull/13)
-   Pull request 14 [handle empty or invalid maxBuildToShow
    parameter](https://github.com/jenkinsci/robot-plugin/pull/14)

#### Release 1.6.2

-   JENKINS-29353 [Configurable number of builds for trends
    generation](https://issues.jenkins-ci.org/browse/JENKINS-29353)

#### Release 1.6.1

-   Reverted earlier caching changes, because sometimes it increased
    memory usage too much
-   JENKINS-29178 [Robot Framework 2.9 output parsing is
    broken](https://issues.jenkins-ci.org/browse/JENKINS-29178)

#### Release 1.6.0

-   Performance enhancements by improved result caching
-   JENKINS-27141 [Option to show only critical tests in
    graphs](https://issues.jenkins-ci.org/browse/JENKINS-27141)
-   JENKINS-26980 [Enable configuration of "Robot Results" column in the
    default job list
    view](https://issues.jenkins-ci.org/browse/JENKINS-26980)
-   JENKINS-24868 [Add the ability for robot plugin graph to show only
    failed
    tests](https://issues.jenkins-ci.org/browse/JENKINS-24868)

#### Release 1.5.0

-   JENKINS-24916 [Link to robot log from individual tests and
    suites](https://issues.jenkins-ci.org/browse/JENKINS-24916)
-   JENKINS-24792 [Cleanup graph
    zooming](https://issues.jenkins-ci.org/browse/JENKINS-24792)
-   JENKINS-24688 [Robot framework summary split in multiple rows in job
    list](https://issues.jenkins-ci.org/browse/JENKINS-24688)

#### Release 1.4.3

-   JENKINS-24531 [There should be high resolution versions of graphs
    available](https://issues.jenkins-ci.org/browse/JENKINS-24531)
-   JENKINS-22406 [Show tags for test
    cases](https://issues.jenkins-ci.org/browse/JENKINS-22406)
-   JENKINS-21739 [Links to robot framework report files are invalid
    when inside a
    folder](https://issues.jenkins-ci.org/browse/JENKINS-21739)
-   JENKINS-21644 [Aggregated results in a multimodule maven project
    displays only values of the last
    submodule](https://issues.jenkins-ci.org/browse/JENKINS-21644)

#### Release 1.4.2

-   JENKINS-22060 Plugin does not count suite setup times into duration
-   JENKINS-21736 Make the archiving of output.xml optional

#### Release 1.4.1

-   JENKINS-21489 URL decoding for test objects fails with special
    characters
-   JENKINS-21490 Split token-macro tokens further to enable more custom
    reporting

#### Release 1.4.0

-   JENKINS-8381 Robot summary and trend graph for multi-configuration
    projects

#### Release 1.3.2

-   JENKINS-19479 Robot Framework Plugin Threshold uses rounded
    percentage for evaluating pass/unstable build status
-   JENKINS-19484 Robot Framework plugin shows result as "Failed!" on
    individual test case result page

#### Release 1.3.1

-   JENKINS-19328 Whole directory is copied when "Other files to copy"
    -field is empty

#### Release 1.3.0

-   JENKINS-15809 Failed to scout
    hudson.plugins.robot.tokens.RobotPassPercentageTokenMacro
-   JENKINS-15188 Human readable duration values for test summaries and
    trend graphs
-   JENKINS-15191 Robot icon is scaled ugly in many places
-   JENKINS-15193 Graph scaling to see the significant changes in test
    count / duration
-   JENKINS-12795 Log/Report link in Job page is broken when Jenkins is
    started with a specific prefix
-   JENKINS-19064 Project/build page summary improvements
-   JENKINS-15768 Plug-in doesn't retain directories when copy files
    using "Other files to copy" setting
-   JENKINS-16615 Link to log.html is broken on project page in matrix
    project
-   JENKINS-18675 Failure in suite teardown does not fail test cases
-   JENKINS-17328 output.xml kept open after build if parsing error
    occurs causing subsequent runs to fail
-   JENKINS-15327 Publish Robot Framework test results not support
    chinese character

#### Release 1.2.3

-   JENKINS-15194 - Basic token macros created for use in e.g. email-ext
    plugin
-   JENKINS-15187 - Made the plugin report tests as unit tests in order
    to see test count in different views e.g. radiator plugins (see
    config notice above)

#### Release 1.2.2

-   Changed XML parsing to much more memory efficient StAX. Should solve
    many outOfMemoryErrors.
-   Changed the way suites with same names are handled
-   Fixed a bug where empty "Other files to copy" field leads to copying
    the whole directory into saved builds.

#### Release 1.2.1

-   Compliance with RF 2.6 log file changes (html and javascript files
    separated)
-   Option to save arbitrary test artifacts with test results

#### Release 1.2

-   configurable file link (to log / report file) on project and build
    pages
-   Listviewcolumn to show overall passed/failed tests in project
    listing
-   pass/fail graph to testcase view
-   support for changed robot output xml (issue:
    <http://www.google.com/url?sa=D&q=http://code.google.com/p/robotframework/issues/detail%3Fid%3D820>)

#### Release 1.1

-   JENKINS-9078 - Jenkins environment variables available for
    configurations
-   JENKINS-9079 - Support for GLOB style wildcards in file
    configurations
-   Detailed views of test cases and suites with trend graphs
-   Fix to JDK 1.5 incompliant exception throw

#### Release 1.0

-   Initial release of the plugin
