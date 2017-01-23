/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.tinkerpop.gremlin.process.actors.traversal.step.map;

import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.actors.ActorProgram;
import org.apache.tinkerpop.gremlin.process.actors.GraphActors;
import org.apache.tinkerpop.gremlin.process.actors.traversal.TraversalActorProgram;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSideEffects;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.AbstractStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.EmptyStep;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.util.TraverserSet;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.javatuples.Pair;

import java.util.NoSuchElementException;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class TraversalActorProgramStep<S, E> extends AbstractStep<E, E> {


    private final Traversal.Admin<S, E> actorsTraversal;
    private final Configuration graphActorsConfiguration;
    private boolean first = true;

    public TraversalActorProgramStep(final Traversal.Admin<?, ?> traversal, final Configuration graphActorsConfiguration) {
        super(traversal);
        this.graphActorsConfiguration = graphActorsConfiguration;
        this.actorsTraversal = (Traversal.Admin) traversal.clone();
        this.actorsTraversal.setParent(EmptyStep.instance());
    }

    @Override
    protected Traverser.Admin<E> processNextStart() throws NoSuchElementException {
        if (this.first) {
            this.first = false;
            try {
                final GraphActors<Pair<TraverserSet<E>, TraversalSideEffects>> graphActors = GraphActors.open(this.graphActorsConfiguration);
                final ActorProgram actorProgram = new TraversalActorProgram<>(this.actorsTraversal);
                final Pair<TraverserSet<E>, TraversalSideEffects> pair = graphActors.program(actorProgram).submit(this.getTraversal().getGraph().get()).get();
                pair.getValue0().forEach(this.starts::add);
                this.getTraversal().setSideEffects(pair.getValue1());
            } catch (final Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
        return this.starts.next();
    }

    @Override
    public String toString() {
        return StringFactory.stepString(this, this.actorsTraversal);
    }

}
