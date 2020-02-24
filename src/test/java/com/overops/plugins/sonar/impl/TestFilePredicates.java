package com.overops.plugins.sonar.impl;

import java.io.File;
import java.net.URI;
import java.util.Collection;

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.api.batch.fs.InputFile.Type;

public class TestFilePredicates implements FilePredicates {

	@Override
	public FilePredicate all() {
		return null;
	}

	@Override
	public FilePredicate none() {
		return null;
	}

	@Override
	public FilePredicate hasAbsolutePath(String s) {
		return null;
	}

	@Override
	public FilePredicate hasRelativePath(String s) {
		return null;
	}

	@Override
	public FilePredicate hasFilename(String s) {
		return null;
	}

	@Override
	public FilePredicate hasExtension(String s) {
		return null;
	}

	@Override
	public FilePredicate hasURI(URI uri) {
		return null;
	}

	@Override
	public FilePredicate matchesPathPattern(String inclusionPattern) {
		return null;
	}

	@Override
	public FilePredicate matchesPathPatterns(String[] inclusionPatterns) {
		return null;
	}

	@Override
	public FilePredicate doesNotMatchPathPattern(String exclusionPattern) {
		return null;
	}

	@Override
	public FilePredicate doesNotMatchPathPatterns(String[] exclusionPatterns) {
		return null;
	}

	@Override
	public FilePredicate hasPath(String s) {
		return null;
	}

	@Override
	public FilePredicate is(File ioFile) {
		return null;
	}

	@Override
	public FilePredicate hasLanguage(String language) {
		return null;
	}

	@Override
	public FilePredicate hasLanguages(Collection<String> languages) {
		return null;
	}

	@Override
	public FilePredicate hasLanguages(String... languages) {
		return null;
	}

	@Override
	public FilePredicate hasType(Type type) {
		return null;
	}

	@Override
	public FilePredicate not(FilePredicate p) {
		return null;
	}

	@Override
	public FilePredicate or(Collection<FilePredicate> or) {
		return null;
	}

	@Override
	public FilePredicate or(FilePredicate... or) {
		return null;
	}

	@Override
	public FilePredicate or(FilePredicate first, FilePredicate second) {
		return null;
	}

	@Override
	public FilePredicate and(Collection<FilePredicate> and) {
		return null;
	}

	@Override
	public FilePredicate and(FilePredicate... and) {
		return null;
	}

	@Override
	public FilePredicate and(FilePredicate first, FilePredicate second) {
		return null;
	}

	@Override
	public FilePredicate hasStatus(Status status) {
		return null;
	}

	@Override
	public FilePredicate hasAnyStatus() {
		return null;
	}

}
