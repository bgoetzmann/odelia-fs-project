package com.odelia.kanban.repository;

import com.odelia.kanban.entity.Card;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

/**
 * Jakarta Data repository for {@link Card}.
 *
 * <p>Every method here is "query by method name": Jakarta Data derives the query
 * from the name, so no implementation and no JPQL is needed. {@code CrudRepository}
 * already provides {@code save}, {@code saveAll}, {@code insert}, {@code findById},
 * {@code delete} and {@code deleteById}.</p>
 */
@Repository(dataStore = "jdbc/kanban")
public interface CardRepository extends CrudRepository<Card, Long> {

    List<Card> findByListIdOrderByPositionAsc(long listId);

    long countByListId(long listId);

    void deleteByListId(long listId);
}
