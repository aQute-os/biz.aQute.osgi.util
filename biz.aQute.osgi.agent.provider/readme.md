# biz.aQute.osgi.agent.provider

An agent for a management system that will go to great lengths to stay alive and follow 
the lead of the management systems. 

## Operations

The agent polls a file from the management agent that contains a _configuration_. This configuration
provides a set of tuples with (location,digest). The agent will compare the required configuration against
the set of installed bundles. It will then uninstall, update, or install the bundles as required. The digest
is used both to verify the download as well as the name of the resource on a CDN server.

If anything files during an upload, the remainder of the actions are aborted, a short delay, and then
the process is re-attempted.

If this still fails after a number of retries then the configuration is ignored and the previous 
configuration is restored. Well, at least that is attempted.

## Strategies

The code has the following strategies implemented as separate classes:

* Downloader – A Downloader that defines how files are downloaded. This is currently very simple a URL. The downloader 
is not doing retries etc. Howeve
* Digest Verifier – The agent algorithm needs to know if a bundle is already installed or not. This decision is delegated to the DigestVerifier.
* TransactionStore – Is used to store the 'current' configuration. Stores this always in a new file with a new name. Remembers
the last n generations and fallbacks. Also verifies that the write is readable.

