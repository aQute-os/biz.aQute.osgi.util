package aQute.jpm.lib;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.lib.io.IO;
import aQute.maven.api.Archive;
import aQute.maven.api.Program;
import aQute.maven.api.Revision;
import aQute.maven.provider.MavenBackingRepository;
import aQute.maven.provider.MavenRepository;
import aQute.maven.provider.POM;
import aQute.service.reporter.Reporter;

class MavenAccess {

	final List<MavenBackingRepository>	snapshots;
	final List<MavenBackingRepository>	releases;
	final List<MavenBackingRepository>	all;
	final PromiseFactory				factory	= new PromiseFactory(null);
	final File							localRepo;
	final MavenRepository				storage;

	MavenAccess(Reporter reporter, String releases, String snapshots, File localRepo, HttpClient client)
		throws Exception {
		if (localRepo == null) {
			this.localRepo = IO.getFile("~/.m2/repository");
		} else
			this.localRepo = localRepo;

		this.snapshots = MavenBackingRepository.create(snapshots, reporter, this.localRepo, client);
		this.releases = MavenBackingRepository.create(releases, reporter, this.localRepo, client);
		this.all = new ArrayList<>(this.releases);
		this.all.addAll(this.snapshots);
		this.storage = new MavenRepository(this.localRepo, "jpm", this.releases, this.snapshots, factory.executor(),
			reporter);
	}

	List<Revision> getRevisions(Program program) throws InterruptedException {
		List<Promise<List<Revision>>> promises = new ArrayList<>();
		for (MavenBackingRepository mbr : all) {
			Promise<List<Revision>> promise = factory.submit(() -> {
				List<Revision> revisions = new ArrayList<>();
				mbr.getRevisions(program, revisions);
				return revisions;
			})
				.recover(p -> Collections.emptyList());
			promises.add(promise);
		}

		List<Revision> revisions = new ArrayList<>();
		for (Promise<List<Revision>> revPromise : promises)
			try {
				revisions.addAll(revPromise.getValue());
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		Collections.sort(revisions);
		return revisions;
	}

	File get(Archive archive) throws InvocationTargetException, InterruptedException, Exception {
		return storage.get(archive)
			.getValue();
	}

	public POM getPom(Revision revision) throws Exception {
		return storage.getPom(revision);
	}

}
