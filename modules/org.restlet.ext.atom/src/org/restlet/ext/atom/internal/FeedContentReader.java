/**
 * Copyright 2005-2009 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or CDL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.ext.atom.internal;

import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.restlet.data.Language;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.engine.util.DateUtils;
import org.restlet.ext.atom.Category;
import org.restlet.ext.atom.Content;
import org.restlet.ext.atom.Entry;
import org.restlet.ext.atom.Feed;
import org.restlet.ext.atom.Link;
import org.restlet.ext.atom.Person;
import org.restlet.ext.atom.Relation;
import org.restlet.ext.atom.Text;
import org.restlet.ext.xml.XmlWriter;
import org.restlet.representation.StringRepresentation;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Content reader for feeds.
 * 
 * @author Thierry Boileau
 */
public class FeedContentReader extends DefaultHandler {
    private enum State {
        FEED, FEED_AUTHOR, FEED_AUTHOR_EMAIL, FEED_AUTHOR_NAME, FEED_AUTHOR_URI, FEED_CATEGORY, FEED_CONTRIBUTOR, FEED_CONTRIBUTOR_EMAIL, FEED_CONTRIBUTOR_NAME, FEED_CONTRIBUTOR_URI, FEED_ENTRY, FEED_ENTRY_AUTHOR, FEED_ENTRY_AUTHOR_EMAIL, FEED_ENTRY_AUTHOR_NAME, FEED_ENTRY_AUTHOR_URI, FEED_ENTRY_CATEGORY, FEED_ENTRY_CONTENT, FEED_ENTRY_CONTRIBUTOR, FEED_ENTRY_ID, FEED_ENTRY_LINK, FEED_ENTRY_PUBLISHED, FEED_ENTRY_RIGHTS, FEED_ENTRY_SOURCE, FEED_ENTRY_SOURCE_AUTHOR, FEED_ENTRY_SOURCE_AUTHOR_EMAIL, FEED_ENTRY_SOURCE_AUTHOR_NAME, FEED_ENTRY_SOURCE_AUTHOR_URI, FEED_ENTRY_SOURCE_CATEGORY, FEED_ENTRY_SOURCE_CONTRIBUTOR, FEED_ENTRY_SOURCE_GENERATOR, FEED_ENTRY_SOURCE_ICON, FEED_ENTRY_SOURCE_ID, FEED_ENTRY_SOURCE_LINK, FEED_ENTRY_SOURCE_LOGO, FEED_ENTRY_SOURCE_RIGHTS, FEED_ENTRY_SOURCE_SUBTITLE, FEED_ENTRY_SOURCE_TITLE, FEED_ENTRY_SOURCE_UPDATED, FEED_ENTRY_SUMMARY, FEED_ENTRY_TITLE, FEED_ENTRY_UPDATED, FEED_GENERATOR, FEED_ICON, FEED_ID, FEED_LINK, FEED_LOGO, FEED_RIGHTS, FEED_SUBTITLE, FEED_TITLE, FEED_UPDATED, NONE
    }

    /** Buffer for the current text content of the current tag. */
    private StringBuilder contentBuffer;

    /** The currently parsed Category. */
    private Category currentCategory;

    /** The currently parsed Content. */
    private Content currentContent;

    /** The currently parsed XML content writer. */
    private XmlWriter currentContentWriter;

    /** The currently date parsed from the current text content. */
    private Date currentDate;

    /** The currently parsed Entry. */
    private Entry currentEntry;

    /** The currently parsed Feed. */
    private final Feed currentFeed;

    /** The currently parsed Link. */
    private Link currentLink;

    /** The currently parsed Person. */
    private Person currentPerson;

    /** The currently parsed Text. */
    private Text currentText;

    /** The current list of prefix mappings. */
    private Map<String, String> prefixMappings;

    /** The currently state. */
    private FeedContentReader.State state;

    /**
     * Constructor.
     * 
     * @param feed
     *            The feed object to update during the parsing.
     */
    public FeedContentReader(Feed feed) {
        this.state = State.NONE;
        this.currentFeed = feed;
        this.currentEntry = null;
        this.currentText = null;
        this.currentDate = null;
        this.currentLink = null;
        this.currentPerson = null;
        this.contentBuffer = null;
        this.currentCategory = null;
        this.currentContent = null;
        this.prefixMappings = new TreeMap<String, String>();
    }

    /**
     * Receive notification of character data.
     * 
     * @param ch
     *            The characters from the XML document.
     * @param start
     *            The start position in the array.
     * @param length
     *            The number of characters to read from the array.
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (this.state == State.FEED_ENTRY_CONTENT) {
            // The content might embed XML elements from various namespaces
            if (this.currentContentWriter != null) {
                this.currentContentWriter.characters(ch, start, length);
            }
        } else {
            this.contentBuffer.append(ch, start, length);
        }
    }

    /**
     * Receive notification of the end of a document.
     */
    @Override
    public void endDocument() throws SAXException {
        this.state = State.NONE;
        this.currentEntry = null;
        this.contentBuffer = null;
    }

    /**
     * Receive notification of the end of an element.
     * 
     * @param uri
     *            The Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace processing is not being
     *            performed.
     * @param localName
     *            The local name (without prefix), or the empty string if
     *            Namespace processing is not being performed.
     * @param qName
     *            The qualified XML name (with prefix), or the empty string if
     *            qualified names are not available.
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (this.currentText != null) {
            this.currentText.setContent(this.contentBuffer.toString());
        }

        if (this.currentDate != null) {
            final String formattedDate = this.contentBuffer.toString();
            final Date parsedDate = DateUtils.parse(formattedDate,
                    DateUtils.FORMAT_RFC_3339);

            if (parsedDate != null) {
                this.currentDate.setTime(parsedDate.getTime());
            } else {
                this.currentDate = null;
            }
        }

        if (uri.equalsIgnoreCase(Feed.ATOM_NAMESPACE)) {
            if (localName.equals("feed")) {
                this.state = State.NONE;
            } else if (localName.equals("title")) {
                if (this.state == State.FEED_TITLE) {
                    this.currentFeed.setTitle(this.currentText);
                    this.state = State.FEED;
                } else if (this.state == State.FEED_ENTRY_TITLE) {
                    this.currentEntry.setTitle(this.currentText);
                    this.state = State.FEED_ENTRY;
                } else if (this.state == State.FEED_ENTRY_SOURCE_TITLE) {
                    this.currentEntry.getSource().setTitle(this.currentText);
                    this.state = State.FEED_ENTRY_SOURCE;
                }
            } else if (localName.equals("updated")) {
                if (this.state == State.FEED_UPDATED) {
                    this.currentFeed.setUpdated(this.currentDate);
                    this.state = State.FEED;
                } else if (this.state == State.FEED_ENTRY_UPDATED) {
                    this.currentEntry.setUpdated(this.currentDate);
                    this.state = State.FEED_ENTRY;
                } else if (this.state == State.FEED_ENTRY_SOURCE_UPDATED) {
                    this.currentEntry.getSource().setUpdated(this.currentDate);
                    this.state = State.FEED_ENTRY_SOURCE;
                }
            } else if (localName.equals("published")) {
                if (this.state == State.FEED_ENTRY_PUBLISHED) {
                    this.currentEntry.setPublished(this.currentDate);
                    this.state = State.FEED_ENTRY;
                }
            } else if (localName.equals("author")) {
                if (this.state == State.FEED_AUTHOR) {
                    this.currentFeed.getAuthors().add(this.currentPerson);
                    this.state = State.FEED;
                } else if (this.state == State.FEED_ENTRY_AUTHOR) {
                    this.currentEntry.getAuthors().add(this.currentPerson);
                    this.state = State.FEED_ENTRY;
                } else if (this.state == State.FEED_ENTRY_SOURCE_AUTHOR) {
                    this.currentEntry.getSource().getAuthors().add(
                            this.currentPerson);
                    this.state = State.FEED_ENTRY_SOURCE;
                }
            } else if (localName.equals("name")) {
                this.currentPerson.setName(this.contentBuffer.toString());

                if (this.state == State.FEED_AUTHOR_NAME) {
                    this.state = State.FEED_AUTHOR;
                } else if (this.state == State.FEED_ENTRY_AUTHOR_NAME) {
                    this.state = State.FEED_ENTRY_AUTHOR;
                } else if (this.state == State.FEED_ENTRY_SOURCE_AUTHOR_NAME) {
                    this.state = State.FEED_ENTRY_SOURCE_AUTHOR;
                }
            } else if (localName.equals("id")) {
                if (this.state == State.FEED_ID) {
                    this.currentFeed.setId(this.contentBuffer.toString());
                    this.state = State.FEED;
                } else if (this.state == State.FEED_ENTRY_ID) {
                    this.currentEntry.setId(this.contentBuffer.toString());
                    this.state = State.FEED_ENTRY;
                } else if (this.state == State.FEED_ENTRY_SOURCE_ID) {
                    this.currentEntry.getSource().setId(
                            this.contentBuffer.toString());
                    this.state = State.FEED_ENTRY_SOURCE;
                }
            } else if (localName.equals("link")) {
                if (this.state == State.FEED_LINK) {
                    this.currentFeed.getLinks().add(this.currentLink);
                    this.state = State.FEED;
                } else if (this.state == State.FEED_ENTRY_LINK) {
                    this.currentEntry.getLinks().add(this.currentLink);
                    this.state = State.FEED_ENTRY;
                } else if (this.state == State.FEED_ENTRY_SOURCE_LINK) {
                    this.currentEntry.getSource().getLinks().add(
                            this.currentLink);
                    this.state = State.FEED_ENTRY_SOURCE;
                }
            } else if (localName.equalsIgnoreCase("entry")) {
                if (this.state == State.FEED_ENTRY) {
                    this.currentFeed.getEntries().add(this.currentEntry);
                    this.state = State.FEED;
                }
            } else if (localName.equals("category")) {
                if (this.state == State.FEED_CATEGORY) {
                    this.currentFeed.getCategories().add(this.currentCategory);
                    this.state = State.FEED;
                } else if (this.state == State.FEED_ENTRY_CATEGORY) {
                    this.currentEntry.getCategories().add(this.currentCategory);
                    this.state = State.FEED_ENTRY;
                } else if (this.state == State.FEED_ENTRY_SOURCE_CATEGORY) {
                    this.currentEntry.getSource().getCategories().add(
                            this.currentCategory);
                    this.state = State.FEED_ENTRY_SOURCE;
                }
            } else if (localName.equalsIgnoreCase("content")) {
                if (this.state == State.FEED_ENTRY_CONTENT) {
                    if (!this.currentEntry.getContent().isExternal()) {
                        currentContent
                                .setInlineContent(new StringRepresentation(
                                        this.currentContentWriter.getWriter()
                                                .toString().trim()));
                    }

                    this.state = State.FEED_ENTRY;
                }
            }
        } else if (this.state == State.FEED_ENTRY_CONTENT) {
            // The content might embed XML elements from various namespaces
            if (this.currentContentWriter != null) {
                this.currentContentWriter.endElement(uri, localName, qName);
            }
        }

        this.currentText = null;
        this.currentDate = null;
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        this.prefixMappings.remove(prefix);
    }

    /**
     * Returns a media type from an Atom type attribute.
     * 
     * @param type
     *            The Atom type attribute.
     * @return The media type.
     */
    private MediaType getMediaType(String type) {
        MediaType result = null;

        if (type == null) {
            // No type defined
        } else if (type.equals("text")) {
            result = MediaType.TEXT_PLAIN;
        } else if (type.equals("html")) {
            result = MediaType.TEXT_HTML;
        } else if (type.equals("xhtml")) {
            result = MediaType.APPLICATION_XHTML;
        } else {
            result = new MediaType(type);
        }

        return result;
    }

    /**
     * Receive notification of the beginning of a document.
     */
    @Override
    public void startDocument() throws SAXException {
        this.contentBuffer = new StringBuilder();
    }

    /**
     * Receive notification of the beginning of an element.
     * 
     * @param uri
     *            The Namespace URI, or the empty string if the element has no
     *            Namespace URI or if Namespace processing is not being
     *            performed.
     * @param localName
     *            The local name (without prefix), or the empty string if
     *            Namespace processing is not being performed.
     * @param qName
     *            The qualified name (with prefix), or the empty string if
     *            qualified names are not available.
     * @param attrs
     *            The attributes attached to the element. If there are no
     *            attributes, it shall be an empty Attributes object. The value
     *            of this object after startElement returns is undefined.
     */
    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        this.contentBuffer.delete(0, this.contentBuffer.length() + 1);

        if (uri.equalsIgnoreCase(Feed.ATOM_NAMESPACE)) {
            if (localName.equals("feed")) {
                this.state = State.FEED;
            } else if (localName.equals("title")) {
                startTextElement(attrs);

                if (this.state == State.FEED) {
                    this.state = State.FEED_TITLE;
                } else if (this.state == State.FEED_ENTRY) {
                    this.state = State.FEED_ENTRY_TITLE;
                } else if (this.state == State.FEED_ENTRY_SOURCE) {
                    this.state = State.FEED_ENTRY_SOURCE_TITLE;
                }
            } else if (localName.equals("updated")) {
                this.currentDate = new Date();

                if (this.state == State.FEED) {
                    this.state = State.FEED_UPDATED;
                } else if (this.state == State.FEED_ENTRY) {
                    this.state = State.FEED_ENTRY_UPDATED;
                } else if (this.state == State.FEED_ENTRY_SOURCE) {
                    this.state = State.FEED_ENTRY_SOURCE_UPDATED;
                }
            } else if (localName.equals("published")) {
                this.currentDate = new Date();

                if (this.state == State.FEED_ENTRY) {
                    this.state = State.FEED_ENTRY_PUBLISHED;
                }
            } else if (localName.equals("author")) {
                this.currentPerson = new Person();

                if (this.state == State.FEED) {
                    this.state = State.FEED_AUTHOR;
                } else if (this.state == State.FEED_ENTRY) {
                    this.state = State.FEED_ENTRY_AUTHOR;
                } else if (this.state == State.FEED_ENTRY_SOURCE) {
                    this.state = State.FEED_ENTRY_SOURCE_AUTHOR;
                }
            } else if (localName.equals("name")) {
                if (this.state == State.FEED_AUTHOR) {
                    this.state = State.FEED_AUTHOR_NAME;
                } else if (this.state == State.FEED_ENTRY_AUTHOR) {
                    this.state = State.FEED_ENTRY_AUTHOR_NAME;
                } else if (this.state == State.FEED_ENTRY_SOURCE_AUTHOR) {
                    this.state = State.FEED_ENTRY_SOURCE_AUTHOR_NAME;
                }
            } else if (localName.equals("id")) {
                if (this.state == State.FEED) {
                    this.state = State.FEED_ID;
                } else if (this.state == State.FEED_ENTRY) {
                    this.state = State.FEED_ENTRY_ID;
                } else if (this.state == State.FEED_ENTRY_SOURCE) {
                    this.state = State.FEED_ENTRY_SOURCE_ID;
                }
            } else if (localName.equals("link")) {
                this.currentLink = new Link();
                this.currentLink.setHref(new Reference(attrs.getValue("",
                        "href")));
                this.currentLink.setRel(Relation.parse(attrs
                        .getValue("", "rel")));
                this.currentLink.setType(new MediaType(attrs.getValue("",
                        "type")));
                this.currentLink.setHrefLang(new Language(attrs.getValue("",
                        "hreflang")));
                this.currentLink.setTitle(attrs.getValue("", "title"));
                final String attr = attrs.getValue("", "length");
                this.currentLink.setLength((attr == null) ? -1L : Long
                        .parseLong(attr));

                if (this.state == State.FEED) {
                    this.state = State.FEED_LINK;
                } else if (this.state == State.FEED_ENTRY) {
                    this.state = State.FEED_ENTRY_LINK;
                } else if (this.state == State.FEED_ENTRY_SOURCE) {
                    this.state = State.FEED_ENTRY_SOURCE_LINK;
                }
            } else if (localName.equalsIgnoreCase("entry")) {
                if (this.state == State.FEED) {
                    this.currentEntry = new Entry();
                    this.state = State.FEED_ENTRY;
                }
            } else if (localName.equals("category")) {
                this.currentCategory = new Category();
                this.currentCategory.setTerm(attrs.getValue("", "term"));
                this.currentCategory.setScheme(new Reference(attrs.getValue("",
                        "scheme")));
                this.currentCategory.setLabel(attrs.getValue("", "label"));

                if (this.state == State.FEED) {
                    this.state = State.FEED_CATEGORY;
                } else if (this.state == State.FEED_ENTRY) {
                    this.state = State.FEED_ENTRY_CATEGORY;
                } else if (this.state == State.FEED_ENTRY_SOURCE) {
                    this.state = State.FEED_ENTRY_SOURCE_CATEGORY;
                }
            } else if (localName.equalsIgnoreCase("content")) {
                if (this.state == State.FEED_ENTRY) {
                    final MediaType type = getMediaType(attrs.getValue("",
                            "type"));
                    final String srcAttr = attrs.getValue("", "src");
                    this.currentContent = new Content();

                    if (srcAttr == null) {
                        // Content available inline
                        StringWriter sw = new StringWriter();
                        currentContentWriter = new XmlWriter(sw);

                        for (String prefix : this.prefixMappings.keySet()) {
                            currentContentWriter.forceNSDecl(
                                    this.prefixMappings.get(prefix), prefix);
                        }
                    } else {
                        // Content available externally
                        this.currentContent.setExternalRef(new Reference(
                                srcAttr));
                        this.currentContent.setExternalType(type);
                    }

                    this.currentEntry.setContent(currentContent);
                    this.state = State.FEED_ENTRY_CONTENT;
                }
            }
        } else if (this.state == State.FEED_ENTRY_CONTENT) {
            // The content might embed XML elements from various namespaces
            if (this.currentContentWriter != null) {
                this.currentContentWriter.startElement(uri, localName, qName,
                        attrs);
            }
        }
    }

    @Override
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        this.prefixMappings.put(prefix, uri);
    }

    /**
     * Receive notification of the beginning of a text element.
     * 
     * @param attrs
     *            The attributes attached to the element.
     */
    public void startTextElement(Attributes attrs) {
        this.currentText = new Text(getMediaType(attrs.getValue("", "type")));
    }
}