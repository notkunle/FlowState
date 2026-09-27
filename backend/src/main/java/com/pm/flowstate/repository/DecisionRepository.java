package com.pm.flowstate.repository;

import com.pm.flowstate.model.StateDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DecisionRepository extends JpaRepository<StateDecision, StateDecision.StateDecisionId> {
}
