package com.example.seosancomplain.domain.admin;

import com.example.seosancomplain.domain.complaint.ComplaintCategory;

public interface CategoryCount {
    ComplaintCategory getCategory();
    long getCnt();
}
