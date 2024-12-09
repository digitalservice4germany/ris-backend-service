import { CommandProps } from "@tiptap/core"
import { Schema, Node as ProsemirrorNode } from "prosemirror-model"
import { EditorState, Transaction } from "prosemirror-state"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"
import { nextTick } from "vue"
import {
  BorderNumber,
  BorderNumberContent,
  BorderNumberNumber,
} from "@/editor/borderNumber"
import removeBorderNumbers from "@/editor/commands/removeBorderNumbers"
import borderNumberService from "@/services/borderNumberService"
import {
  createDocWithBorderNumber,
  createDocWithEmptyBorderNumber,
} from "~/test-helper/dataGenerators"

const schema = new Schema({
  nodes: {
    doc: { content: "block+" },
    paragraph: { content: "text*", group: "block" },
    text: { inline: true },
    borderNumber: BorderNumber,
    borderNumberNumber: BorderNumberNumber,
    borderNumberContent: BorderNumberContent,
  },
})

vi.mock("vue", () => ({
  nextTick: vi.fn(() => Promise.resolve()),
}))

describe("removeBorderNumbers command", () => {
  const dispatch = vi.fn()

  beforeEach(() => {
    vi.spyOn(
      borderNumberService,
      "makeBorderNumbersSequential",
    ).mockImplementation(() => vi.fn())
    vi.spyOn(
      borderNumberService,
      "invalidateBorderNumberLinks",
    ).mockImplementation(() => vi.fn())
    dispatch.mockClear()
  })
  afterEach(() => vi.restoreAllMocks())

  it("should remove borderNumber nodes and dispatch transaction", async () => {
    // Arrange
    const commandProps: {
      state: EditorState
      dispatch: ((tr: Transaction) => void) | undefined
    } = {
      state: EditorState.create({
        doc: createDocWithBorderNumber(schema),
        schema,
      }),
      dispatch,
    }

    // Act
    const result = removeBorderNumbers(commandProps as CommandProps)

    // Assert
    const dispatchedTransaction: Transaction = dispatch.mock.calls[0][0]
    const isReplaceStep =
      dispatchedTransaction.steps[0].toJSON().stepType === "replace"
    expect(isReplaceStep).toBe(true)
    expect(result).toBe(true)
    expect(dispatch).toHaveBeenCalled()
    await nextTick()
    expect(borderNumberService.makeBorderNumbersSequential).toHaveBeenCalled()
    expect(
      borderNumberService.invalidateBorderNumberLinks,
    ).toHaveBeenCalledWith(["1"])
  })

  it("should not modify the document if no borderNumber nodes are present", async () => {
    // Arrange
    const commandProps: {
      state: EditorState
      dispatch: ((tr: Transaction) => void) | undefined
    } = {
      state: EditorState.create({
        doc: schema.nodes.doc.createAndFill({}, [
          schema.nodes.paragraph.create(
            {},
            schema.text("This is a simple paragraph"),
          ),
        ]) as ProsemirrorNode,
        schema,
      }),
      dispatch,
    }

    // Act
    const result = removeBorderNumbers(commandProps as CommandProps)

    // Assert
    expect(result).toBe(false)
    expect(dispatch).not.toHaveBeenCalled()
    await nextTick()
    expect(borderNumberService.makeBorderNumbersSequential).toHaveBeenCalled()
    expect(
      borderNumberService.invalidateBorderNumberLinks,
    ).not.toHaveBeenCalled()
  })

  it("should remove an empty borderNumber node", async () => {
    // Arrange
    const commandProps: {
      state: EditorState
      dispatch: ((tr: Transaction) => void) | undefined
    } = {
      state: EditorState.create({
        doc: createDocWithEmptyBorderNumber(schema),
        schema,
      }),
      dispatch,
    }

    // Act
    const result = removeBorderNumbers(commandProps as CommandProps)

    // Assert
    expect(result).toBe(true)
    expect(dispatch).toHaveBeenCalled()
    await nextTick()
    expect(borderNumberService.makeBorderNumbersSequential).toHaveBeenCalled()
    expect(
      borderNumberService.invalidateBorderNumberLinks,
    ).toHaveBeenCalledWith(["1"])
  })
})
