package com.odelia.kanban.repository;

import com.odelia.kanban.entity.BoardList;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;

import java.util.List;

/**
 * Jakarta Data repository for {@link BoardList}.
 *
 * <p>{@code findByBoardIdOrderByPositionAsc} and {@code countByBoardId} are
 * "query by method name": Jakarta Data derives the query from the method name,
 * no implementation and no JPQL required.</p>
 */
@Repository(dataStore = "jdbc/kanban")
public interface BoardListRepository extends CrudRepository<BoardList, Long> {

    List<BoardList> findByBoardIdOrderByPositionAsc(long boardId);

    long countByBoardId(long boardId);

    void deleteByBoardId(long boardId);
}
