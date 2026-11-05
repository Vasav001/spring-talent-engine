package com.vasav.springtalentengine.jobs;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "skill_synonyms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SkillSynonym {

    @Id
    private String alias;

    private String canonical;
}
