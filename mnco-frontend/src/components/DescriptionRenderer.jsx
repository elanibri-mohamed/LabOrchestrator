import React from 'react';

const IMAGE_TOKEN_REGEX = /!\[([^\]]*)\]\((data:[^)]+)\)/g;

export const insertImageTokenAtCursor = (currentValue, selectionStart, selectionEnd, dataUrl, alt = 'pasted image') => {
  const token = `![${alt}](${dataUrl})`;
  return {
    value: `${currentValue.slice(0, selectionStart)}${token}${currentValue.slice(selectionEnd)}`,
    token,
  };
};

const renderTextSegment = (segment, keyPrefix) => {
  const lines = segment.split('\n');
  return lines.map((line, index) => (
    <React.Fragment key={`${keyPrefix}-line-${index}`}>
      {line}
      {index < lines.length - 1 ? <br /> : null}
    </React.Fragment>
  ));
};

const DescriptionRenderer = ({ text, className }) => {
  if (!text) {
    return <p className={className}>No description added yet.</p>;
  }

  const parts = [];
  let lastIndex = 0;
  let match;
  IMAGE_TOKEN_REGEX.lastIndex = 0;

  while ((match = IMAGE_TOKEN_REGEX.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ type: 'text', value: text.slice(lastIndex, match.index) });
    }

    parts.push({ type: 'image', alt: match[1] || 'pasted image', src: match[2] });
    lastIndex = IMAGE_TOKEN_REGEX.lastIndex;
  }

  if (lastIndex < text.length) {
    parts.push({ type: 'text', value: text.slice(lastIndex) });
  }

  return (
    <div className={className}>
      {parts.map((part, index) => {
        if (part.type === 'image') {
          return (
            <img
              key={`desc-image-${index}`}
              src={part.src}
              alt={part.alt}
              className="description-inline-image"
            />
          );
        }

        return <React.Fragment key={`desc-text-${index}`}>{renderTextSegment(part.value, `desc-${index}`)}</React.Fragment>;
      })}
    </div>
  );
};

export default DescriptionRenderer;
