import org.crsh.command.CRaSHCommand
import org.crsh.cmdline.annotations.Usage
import org.crsh.cmdline.annotations.Command
import org.crsh.cmdline.annotations.Argument
import org.crsh.cmdline.annotations.Required
import org.crsh.text.ui.UIBuilder
import org.crsh.cmdline.spi.Completer
import org.crsh.cmdline.spi.Completion
import org.crsh.cmdline.ParameterDescriptor
import org.crsh.cmdline.OptionDescriptor

import javax.management.MBeanServerFactory
import javax.management.MBeanServer
import javax.management.ObjectName

@Usage("mule commands")
class mule extends CRaSHCommand implements Completer {
  MBeanServer mbeanServer = getMBeanServer()

  @Usage("print information about the broker")
  @Command
  void info() {
      def muleContextMBean = getFirstMuleContextMBean()
      if (muleContextMBean == null) {
          out << "Failed to locate any MuleContext MBean - report this issue to the developers of this command\n"
      } else {
          context.provide([name:"Mule Version",value:muleContextMBean.Version])
          context.provide([name:"Build Number",value:muleContextMBean.BuildNumber])
          context.provide([name:"Build Date",value:muleContextMBean.BuildDate])
          context.provide([name:"Host IP",value:muleContextMBean.HostIp])
          context.provide([name:"Hostname",value:muleContextMBean.Hostname])
          context.provide([name:"OS",value:muleContextMBean.OsVersion])
          context.provide([name:"JDK",value:muleContextMBean.JdkVersion])
      }
  }

  @Usage("list the names of all deployed applications")
  @Command
  void apps() {
      getApplicationNames().each { appName ->
          def muleContextMBean = new GroovyMBean(mbeanServer, "Mule.$appName:name=MuleContext")
          context.provide([name:appName,'start time':muleContextMBean.StartTime])
      }
  }

  @Usage("print the statistics for an application or a flow within an application")
  @Command
  void stats(@Usage("The application name") @Required @Option(names=["a"], completer=mule.class) String applicationName,
             @Usage("The flow name") @Option(names=["f"]) String flowName) {

      try {
          def statisticsMBeanObjectName =
              "Mule.$applicationName:type=org.mule.Statistics,"
              .plus(flowName != null ? "Flow=$flowName" : 'Application=application totals')

          def statisticsMBean = new GroovyMBean(mbeanServer, statisticsMBeanObjectName)
          statisticsMBean.listAttributeNames().each { statisticName ->
              context.provide([name:statisticName,value:statisticsMBean[statisticName]])
          }
      } catch (Exception e) {
          out << "Failed to locate statistics for the provided parameters ($e.message)\n"
      }
  }

  @Usage("list all the flows of an application")
  @Command
  void flows(@Usage("The application name") @Required @Option(names=["a"], completer=mule.class) String applicationName) {
      listMBeans(applicationName, 'Flow', { mBean -> 
          [name:mBean.Name,type:mBean.Type]
      })
  }

  @Usage("list all the connectors of an application")
  @Command
  void connectors(@Usage("The application name") @Required @Option(names=["a"], completer=mule.class) String applicationName) {
      listMBeans(applicationName, 'Connector', { mBean -> 
          [name:mBean.Name, protocol:mBean.Protocol,started:mBean.Started,disposed:mBean.Disposed]
      })
  }

  @Usage("list all the endpoints of an application")
  @Command
  void endpoints(@Usage("The application name") @Required @Option(names=["a"], completer=mule.class) String applicationName) {
      listMBeans(applicationName, 'Endpoint', { mBean -> 
          [name:mBean.Name,address:mBean.Address,flow:mBean.ComponentName,inbound:mBean.Inbound,outbound:mBean.Outbound,connected:mBean.Connected]
      })
  }

  private void listMBeans(String applicationName, String type, Closure extractor) {
      def mBeanPattern = new ObjectName("Mule.$applicationName:type=$type,*")
      mbeanServer.queryNames(mBeanPattern, null).sort().each { objectName ->
          def mBean = new GroovyMBean(mbeanServer, objectName)
          context.provide(extractor(mBean))
      }
  }

  // TODO control one app
  // TODO control one flow
  // TODO control one connector
  // TODO control one endpoint

  Completion complete(ParameterDescriptor<?> parameter, String prefix) {
      if (parameter instanceof OptionDescriptor && parameter.names.contains("a")) {
          def completionBuilder = Completion.builder(prefix)
          getApplicationNames().findAll { appName ->
              appName.startsWith(prefix)
          }.each { appName ->
              completionBuilder.add(appName.substring(prefix.length()), true)
          }
          completionBuilder.build()
      } else {
          Completion.create(prefix);
      }
  }

  private List<String> getApplicationNames() {
      mbeanServer.domains.sort().findAll { domain ->
          isMuleApplicationDomain(domain)
      }.collect { domain ->
          domain.substring(5)
      }
  }

  private GroovyMBean getFirstMuleContextMBean() {
      for (domain in mbeanServer.domains) {
          if (isMuleApplicationDomain(domain)) {
              return new GroovyMBean(mbeanServer, "$domain:name=MuleContext")
          }
      }
      return null
  }

  private boolean isMuleApplicationDomain(String domain) {
      domain ==~ /Mule\..+/
  }

  private MBeanServer getMBeanServer() {
      MBeanServerFactory.findMBeanServer(null)[0]
  }
}