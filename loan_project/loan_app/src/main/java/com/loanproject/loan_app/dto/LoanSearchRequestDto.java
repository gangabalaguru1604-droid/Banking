package com.loanproject.loan_app.dto;

public class LoanSearchRequestDto {

    private String searchType = "ACCOUNT_NUMBER";
    private String query;
    private int page = 0;
    private int size = 10;

    public LoanSearchRequestDto() {
    }

    public LoanSearchRequestDto(String searchType, String query, int page, int size) {
        this.searchType = searchType;
        this.query = query;
        this.page = page;
        this.size = size;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
