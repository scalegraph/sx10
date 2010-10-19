/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.ast;

import java.util.List;

/**
 * An immutable representation of a <code>try</code> block, one or more
 * <code>catch</code> blocks, and an optional <code>finally</code> block.
 *
 */
public interface Try extends CompoundStmt
{
    /** The block to "try". */
    Block tryBlock();

    /** Set the block to "try". */
    Try tryBlock(Block tryBlock);

    /** List of catch blocks.
     * @return A list of {@link polyglot.ast.Catch Catch}.
     */
    List<Catch> catchBlocks();

    /** Set the list of catch blocks.
     * @param catchBlocks A list of {@link polyglot.ast.Catch Catch}.
     */
    Try catchBlocks(List<Catch> catchBlocks);

    /** The block to "finally" execute. */
    Block finallyBlock();

    /** Set the block to "finally" execute. */
    Try finallyBlock(Block finallyBlock);
}
