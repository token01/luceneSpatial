package com.lucene.pojo;

import java.util.ArrayList;

public class SearchResult {
	public int status;
	public int total;
	public ArrayList<String> result = new ArrayList<>();

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public ArrayList<String> getResult() {
		return result;
	}

	public void setResult(ArrayList<String> result) {
		this.result = result;
	}

}
