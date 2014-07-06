/**
 * Copyright (c) 2009-2014, rultor.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met: 1) Redistributions of source code must retain the above
 * copyright notice, this list of conditions and the following
 * disclaimer. 2) Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution. 3) Neither the name of the rultor.com nor
 * the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.rultor.agents.merge;

import com.jcabi.aspects.Immutable;
import com.jcabi.log.Logger;
import com.jcabi.xml.XML;
import com.rultor.agents.AbstractAgent;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.xembly.Directive;
import org.xembly.Directives;

/**
 * Merges.
 *
 * @author Yegor Bugayenko (yegor@tpc2.com)
 * @version $Id$
 * @since 1.0
 */
@Immutable
public final class StartsGitMerge extends AbstractAgent {

    /**
     * Command to run in order to validate.
     */
    private final transient String cmd;

    /**
     * Ctor.
     * @param command Command to run
     */
    public StartsGitMerge(final String command) {
        super(
            "/talk/merge-request-git[not(success)]",
            "/talk[not(daemon)]"
        );
        this.cmd = command;
    }

    @Override
    public Iterable<Directive> process(final XML xml) {
        final XML req = xml.nodes("//merge-request-git").get(0);
        final String script = StringUtils.join(
            Arrays.asList(
                String.format(
                    "git clone %s repo",
                    req.xpath("base/text()").get(0)
                ),
                "cd repo",
                String.format(
                    "git checkout %s",
                    req.xpath("base-branch/text()").get(0)
                ),
                String.format(
                    "git remote add head %s",
                    req.xpath("head/text()").get(0)
                ),
                "git remote update",
                String.format(
                    "git merge head/%s",
                    req.xpath("head-branch/text()").get(0)
                ),
                String.format(
                    "sudo docker -rm -v .:/main -w=/main yegor256/rultor %s",
                    this.cmd
                ),
                String.format(
                    "git push origin %s",
                    req.xpath("base-branch/text()").get(0)
                )
            ),
            "\n"
        );
        final String hash = req.xpath("@id").get(0);
        Logger.info(this, "git merge started: %s", hash);
        return new Directives().xpath("/talk")
            .add("daemon")
            .attr("id", hash)
            .add("script").set(script);
    }
}