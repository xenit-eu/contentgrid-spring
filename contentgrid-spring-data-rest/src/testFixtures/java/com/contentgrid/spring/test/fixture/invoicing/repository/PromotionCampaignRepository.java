package com.contentgrid.spring.test.fixture.invoicing.repository;

import com.contentgrid.spring.test.fixture.invoicing.model.PromotionCampaign;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "promotions")
public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, UUID>,
        QuerydslPredicateExecutor<PromotionCampaign> {

    Optional<PromotionCampaign> findByPromoCode(String code);
}
