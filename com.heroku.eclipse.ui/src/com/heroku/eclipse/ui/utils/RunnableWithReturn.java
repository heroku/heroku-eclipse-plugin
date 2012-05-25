package com.heroku.eclipse.ui.utils;

public interface RunnableWithReturn<R,A> {
	public R run(A argument);
}
