package pe.victoryordi.paycore.adapter.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.victoryordi.paycore.adapter.out.persistence.entity.PaymentJpaEntity;

import java.util.UUID;

interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, Long> {

    Page<PaymentJpaEntity> findByMerchantIdOrderByCreatedAtDesc(UUID merchantId, Pageable pageable);

    java.util.Optional<PaymentJpaEntity> findByPublicId(UUID publicId);
}
