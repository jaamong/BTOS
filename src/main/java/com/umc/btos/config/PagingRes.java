package com.umc.btos.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagingRes {
    private boolean hasNext;
    private int currentPage;
    private int startPage = 1;
    private int endPage;
    private int dataPerPage;
    private int dataNum; // 데이터 총 개수

    public PagingRes(int currentPage, int dataPerPage) {
        this.currentPage = currentPage;
        this.dataPerPage = dataPerPage;
    }

}
