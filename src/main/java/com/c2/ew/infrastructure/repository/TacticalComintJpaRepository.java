package com.c2.ew.infrastructure.repository;

import com.c2.ew.domain.enums.*;
import com.c2.ew.infrastructure.entity.TacticalComintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TacticalComintJpaRepository extends JpaRepository<TacticalComintEntity, String>, JpaSpecificationExecutor<TacticalComintEntity> {

    long countByTeshisKimlik(TeshisKimlik teshisKimlik);

    long countByHaberlesmeSekli(HaberlesmeSekli haberlesmeSekli);

    long countByTip(MuhabereBantTipi tip);

    long countByVeriKaynagi(VeriKaynagi veriKaynagi);

    List<TacticalComintEntity> findByCagriAdiContainingIgnoreCaseOrKarsiCagriAdiContainingIgnoreCase(String cagriAdi, String karsiCagriAdi);

    @Query("SELECT c.teshisKimlik, COUNT(c) FROM TacticalComintEntity c GROUP BY c.teshisKimlik")
    List<Object[]> getTeshisDistribution();

    @Query("SELECT c.haberlesmeSekli, COUNT(c) FROM TacticalComintEntity c GROUP BY c.haberlesmeSekli")
    List<Object[]> getHaberlesmeSekliDistribution();

    @Query("SELECT c.tip, COUNT(c) FROM TacticalComintEntity c GROUP BY c.tip")
    List<Object[]> getTipDistribution();

    @Query("SELECT c.lisan, COUNT(c) FROM TacticalComintEntity c GROUP BY c.lisan")
    List<Object[]> getLisanDistribution();

    @Query("SELECT c.veriKaynagi, COUNT(c) FROM TacticalComintEntity c GROUP BY c.veriKaynagi")
    List<Object[]> getVeriKaynagiDistribution();
}
