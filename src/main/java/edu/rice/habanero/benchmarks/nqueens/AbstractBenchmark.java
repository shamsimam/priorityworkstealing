/**
 * *************************************************************************************************************************************************************************************
 */
/*  This program is part of the Barcelona OpenMP Tasks Suite                                  */
/*  Copyright (C) 2009 Barcelona Supercomputing Center - Centro Nacional de Supercomputacion  */
/*  Copyright (C) 2009 Universitat Politecnica de Catalunya                                   */
/*                                                                                            */
/*  This program is free software; you can redistribute it and/or modify                      */
/*  it under the terms of the GNU General Public License as published by                      */
/*  the Free Software Foundation; either version 2 of the License, or                         */
/*  (at your option) any later version.                                                       */
/*                                                                                            */
/*  This program is distributed in the hope that it will be useful,                           */
/*  but WITHOUT ANY WARRANTY; without even the implied warranty of                            */
/*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                             */
/*  GNU General Public License for more details.                                              */
/*                                                                                            */
/*  You should have received a copy of the GNU General Public License                         */
/*  along with this program; if not, write to the Free Software                               */
/*  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA            */
/**********************************************************************************************/

/* Original code from the Application Kernel Matrix by Cray */

package edu.rice.habanero.benchmarks.nqueens;

import edu.rice.habanero.benchmarks.Benchmark;
import edu.rice.habanero.concurrent.executors.TaskExecutor;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static edu.rice.habanero.benchmarks.nqueens.NQueensConfig.extendRight;
import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.async;
import static edu.rice.habanero.concurrent.util.TaskExecutorUtil.kernel;

/**
 * @author <a href="http://shams.web.rice.edu/">Shams Imam</a> (shams@rice.edu) [HJlib version]
 */
public abstract class AbstractBenchmark extends Benchmark {

    private final AtomicLong resultCounter = new AtomicLong(0);

    /**
     * package protected constructor.
     */
    AbstractBenchmark() {
        super();
    }

    @Override
    public void initialize(final String[] args) throws IOException {
        NQueensConfig.parseArgs(args);
    }

    @Override
    public void printArgInfo() {
        NQueensConfig.printArgs();
    }

    @Override
    public void preIteration(final boolean firstIteration) {
        resultCounter.set(0);
    }

    protected abstract TaskExecutor createTaskExecutor();

    @Override
    public void runIteration() {
        final TaskExecutor taskExecutor = createTaskExecutor();
        kernel(taskExecutor, new Runnable() {
            @Override
            public void run() {
                final int[] a = new int[0];
                nqueensKernelPar(a, 0);
            }

            private void nqueensKernelPar(final int[] a, final int depth) {

                for (int i = 0; i < NQueensConfig.SIZE; i++) {
                    final int ii = i;
                    final int[] b = extendRight(a, ii);
                    async(depth, new Runnable() {
                        @Override
                        public void run() {
                            {
                                // termination check on entry
                                final long value = resultCounter.get();
                                if (value >= NQueensConfig.SOLUTIONS_LIMIT) {
                                    return;
                                }
                            }

                            if (NQueensConfig.boardValid((depth + 1), b)) {
                                if (depth < NQueensConfig.THRESHOLD) {
                                    nqueensKernelPar(b, depth + 1);
                                } else {
                                    final int[] b2 = new int[NQueensConfig.SIZE];
                                    System.arraycopy(b, 0, b2, 0, b.length);
                                    nqueensKernelSeq(b2, depth + 1);
                                }
                            }
                        }
                    });
                }
            }

            private void nqueensKernelSeq(final int[] a, final int depth) {

                if (NQueensConfig.SIZE == depth) {
                    resultCounter.addAndGet(1L);
                    return;
                }

                for (int i = 0; i < NQueensConfig.SIZE; i++) {
                    a[depth] = i;
                    if (NQueensConfig.boardValid((depth + 1), a)) {
                        nqueensKernelSeq(a, depth + 1);
                        {
                            // cooperative termination check
                            final long value = resultCounter.get();
                            if (value >= NQueensConfig.SOLUTIONS_LIMIT) {
                                return;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void cleanupIteration(final boolean lastIteration, final double execTimeMillis) {
        NQueensConfig.validate(this, resultCounter.get());
    }

}
