package com.minsproject.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class SchWord {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(length = 5, nullable = false)
    private Long id;
	
	
    @Column(length = 8, nullable = false)
    private String schDate;
    
    
    @Column(length = 500, nullable = false)
    private String schWord;
    
    
    @Builder
    public SchWord(String schDate, String schWord) {
        this.schDate = schDate;
        this.schWord = schWord;
    }

}



