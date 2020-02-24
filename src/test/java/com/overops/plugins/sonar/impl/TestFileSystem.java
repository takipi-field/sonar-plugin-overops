package com.overops.plugins.sonar.impl;

import java.io.File;
import java.nio.charset.Charset;
import java.util.SortedSet;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;

public class TestFileSystem implements FileSystem {

	public TestFilePredicates filePredicates = new TestFilePredicates();
	public TestInputFile sourceFile = new TestInputFile();

	@Override
	public File baseDir() {
		return null;
	}

	@Override
	public Charset encoding() {
		return null;
	}

	@Override
	public File workDir() {
		return null;
	}

	@Override
	public FilePredicates predicates() {
		return filePredicates;
	}

	@Override
	public InputFile inputFile(FilePredicate predicate) {
		return sourceFile;
	}

	@Override
	public InputDir inputDir(File dir) {
		return null;
	}

	@Override
	public Iterable<InputFile> inputFiles(FilePredicate predicate) {
		return null;
	}

	@Override
	public boolean hasFiles(FilePredicate predicate) {
		return false;
	}

	@Override
	public Iterable<File> files(FilePredicate predicate) {
		return null;
	}

	@Override
	public SortedSet<String> languages() {
		return null;
	}

	@Override
	public File resolvePath(String path) {
		return null;
	}

}
