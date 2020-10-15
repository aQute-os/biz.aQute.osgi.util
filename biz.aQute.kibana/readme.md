# Kibana 

Elastic Search is a no-schema database for humongous amounts of data. Kibana is a visualization tool that leverages
the features of Elastic Search.
  
Kibana provides standard tools to upload data to Elastic Search: [beats][1] and [logstash][2]. Although these are very useful, they 
immediately make the application depend on native programs. Although that is often a quick and dirty solution,
it does imply deployment and portability issues. The beauty of Java is that it runs anywhere.

Therefore, this bundle acts as a log forwarder to Kibana, i.e. it works like a beat. It can be configured with a number of 
URL's to an elastic search cluster. It will then listen to OSGi log entries and forward them to elastic search. A short
delay can be configured to aggregate a number of log records per request for efficiency.

## Trying it out

You can create a [free Eleastic Search trial account][3]. You then need to create a _deployment_ in the cloud. This
deployment starts up a cluster of Elastic Search nodes. This bundles needs the URL to the Elastic Search engine,
which you can copy from the [home page][4], select your deployment, and then copy the _Elastic Search_ endpoint. 

The `bnd.bnd` file in this project contains a run configuration. You can select it, then the `Run` tab, and then click 
on the `Run` icon in the right top. Then goto: [http://localhost:8080/system/console/configMgr](http://localhost:8080/system/console/configMgr).
Click on the `+` at the line with `Biz a qute kibana kibana log uploader configuration`. This creates a new
configuration record. The form you see should make the parameters clear.

* Hosts – One or more URLs to the Elastic Search engines. Each URL is tried in turn.
* Password – A password for your account.
* User id – The user id of the account, this was `elastic` for me.
* Delay – Number of seconds to wait before pushing a log record. Logs are aggregated so this is more efficient if you make it longer.
* Index – The name of the log. This is normally a gateway specific name. The Kibaba convention is to start with `logs-`. I suggest then
  the name of the gateway, and then `osgi`. I.e. `logs-GW5124232-osgi`.
  
You can create multiple Kibaba Log Forwarders.

## Configuration

* The PID is `biz.aQute.kibana`. 
* Configuration is defined in [biz.aQute.kibana.KibanaLogUploader$Configuration](src/main/java/biz/aQute/kibana/KibanaLogUploader#Configuration)


[1]: https://www.elastic.co/beats/
[2]: https://www.elastic.co/guide/en/kibana/current/logstash-page.html
[3]: https://www.elastic.co/downloads/
[4]: https://cloud.elastic.co/home