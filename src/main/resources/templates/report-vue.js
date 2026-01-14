// Pilaf Test Report - Vue.js Application
// Uses Vue 3 Composition API with CDN-based setup
// Handles JSON tree rendering with w-jsonview-tree and diffs with jsondiffpatch

(function() {
  'use strict';

  const { createApp, ref, reactive, computed, onMounted, nextTick, watch } = Vue;

  const PilafReport = {
    setup() {
      // ============================================
      // REACTIVE STATE
      // ============================================
      const report = ref({});
      const stories = ref([]);
      const expandedStories = reactive({});
      const expandedResponses = reactive({});
      const stateViews = reactive({});

      // Refs for DOM elements that need imperative updates
      const responseRefs = reactive({});
      const diffRefs = reactive({});
      const beforeTreeRefs = reactive({});
      const afterTreeRefs = reactive({});

      // Track rendered state
      const renderedResponses = reactive({});
      const renderedDiffs = reactive({});
      const jsondiffpatchReady = ref(false);

      // ============================================
      // COMPUTED PROPERTIES
      // ============================================
      const totalSteps = computed(() =>
        stories.value.reduce((sum, story) => sum + (story.steps?.length || 0), 0)
      );

      const passedSteps = computed(() =>
        stories.value.reduce((sum, story) =>
          sum + (story.steps?.filter(s => s.passed).length || 0), 0)
      );

      const failedSteps = computed(() =>
        stories.value.reduce((sum, story) =>
          sum + (story.steps?.filter(s => !s.passed).length || 0), 0)
      );

      // ============================================
      // REF SETTERS (for template refs)
      // ============================================
      const setResponseRef = (storyIndex, stepIndex, el) => {
        if (el) {
          const key = `${storyIndex}-${stepIndex}`;
          responseRefs[key] = el;
        }
      };

      const setDiffRef = (storyIndex, stepIndex, el) => {
        if (el) {
          const key = `${storyIndex}-${stepIndex}`;
          diffRefs[key] = el;
        }
      };

      const setBeforeTreeRef = (storyIndex, stepIndex, el) => {
        if (el) {
          const key = `${storyIndex}-${stepIndex}`;
          beforeTreeRefs[key] = el;
        }
      };

      const setAfterTreeRef = (storyIndex, stepIndex, el) => {
        if (el) {
          const key = `${storyIndex}-${stepIndex}`;
          afterTreeRefs[key] = el;
        }
      };

      // ============================================
      // STORY TOGGLE METHODS
      // ============================================
      const toggleStory = (index) => {
        expandedStories[index] = !expandedStories[index];

        // Render responses and diffs when story is expanded
        if (expandedStories[index]) {
          nextTick(() => {
            const story = stories.value[index];
            if (story && story.steps) {
              story.steps.forEach((step, stepIndex) => {
                const key = `${index}-${stepIndex}`;

                // Render response if exists and not already rendered
                if (step.actual && !renderedResponses[key]) {
                  renderResponse(key, step.actual);
                }

                // Render diff if states exist and not already rendered
                if ((step.stateBefore || step.stateAfter) && !renderedDiffs[key]) {
                  renderDiff(key, step.stateBefore, step.stateAfter);
                }
              });
            }
          });
        }
      };

      const expandAll = () => {
        stories.value.forEach((_, index) => {
          expandedStories[index] = true;
        });

        // Render all content
        nextTick(() => renderAllContent());
      };

      const collapseAll = () => {
        stories.value.forEach((_, index) => {
          expandedStories[index] = false;
        });
      };

      // ============================================
      // RESPONSE TOGGLE METHODS
      // ============================================
      const toggleResponse = (stepKey) => {
        expandedResponses[stepKey] = !expandedResponses[stepKey];
      };

      // ============================================
      // STATE VIEW METHODS
      // ============================================
      const setStateView = (stepKey, view) => {
        stateViews[stepKey] = view;

        // Render full view trees if switching to full view
        if (view === 'full') {
          nextTick(() => {
            const [storyIndex, stepIndex] = stepKey.split('-').map(Number);
            const story = stories.value[storyIndex];
            if (story && story.steps && story.steps[stepIndex]) {
              const step = story.steps[stepIndex];
              renderTreeView(stepKey, step.stateBefore, step.stateAfter);
            }
          });
        }
      };

      const getStateView = (stepKey) => {
        return stateViews[stepKey] || 'compare';
      };

      // ============================================
      // RENDERING METHODS
      // ============================================

      /**
       * Renders all responses and diffs for visible content
       */
      const renderAllContent = () => {
        stories.value.forEach((story, storyIndex) => {
          if (story.steps && expandedStories[storyIndex]) {
            story.steps.forEach((step, stepIndex) => {
              const key = `${storyIndex}-${stepIndex}`;

              if (step.actual && !renderedResponses[key]) {
                renderResponse(key, step.actual);
              }

              if ((step.stateBefore || step.stateAfter) && !renderedDiffs[key]) {
                renderDiff(key, step.stateBefore, step.stateAfter);
              }
            });
          }
        });
      };

      /**
       * Renders a JSON response using w-jsonview-tree
       */
      const renderResponse = (key, response) => {
        if (!response) return;

        const container = responseRefs[key];
        if (!container) {
          console.log('Response container not ready yet:', key);
          return;
        }

        try {
          let data;
          if (typeof response === 'string') {
            data = JSON.parse(response);
          } else {
            data = response;
          }

          container.innerHTML = '';
          container.classList.add('CompCssDJsonViewTree');

          if (window['w-jsonview-tree']) {
            window['w-jsonview-tree'](data, container, { expanded: false });
            renderedResponses[key] = true;
          } else {
            // Fallback to formatted JSON
            container.innerHTML = '<pre class="json-tree">' + escapeHtml(JSON.stringify(data, null, 2)) + '</pre>';
            renderedResponses[key] = true;
          }
        } catch (e) {
          console.log('Failed to parse response for', key, ':', e.message);
          container.innerHTML = '<pre class="text-gray-400">' + escapeHtml(String(response)) + '</pre>';
          renderedResponses[key] = true;
        }
      };

      /**
       * Renders a JSON diff using jsondiffpatch
       */
      const renderDiff = (key, before, after) => {
        if (!jsondiffpatchReady.value && !window.jsondiffpatch) {
          console.log('jsondiffpatch not ready yet, will retry');
          return;
        }

        // Try to get container from refs, fall back to getElementById
        let container = diffRefs[key];
        if (!container) {
          // Try to find by ID (format: diff-container-storyIndex-stepIndex)
          const containerId = 'diff-container-' + key;
          container = document.getElementById(containerId);
        }
        if (!container) {
          console.log('Diff container not found:', key);
          return;
        }

        // If before/after not provided, try to get from data attributes
        if (!before || !after) {
          const dataBefore = container.getAttribute('data-before');
          const dataAfter = container.getAttribute('data-after');
          before = before || (dataBefore && dataBefore.trim() ? dataBefore : null);
          after = after || (dataAfter && dataAfter.trim() ? dataAfter : null);
        }

        // If still no data, try to get from embedded report JSON
        if ((!before || !after) && report.value && report.value.stories) {
          const [storyIndex, stepIndex] = key.split('-').map(Number);
          const story = report.value.stories[storyIndex];
          if (story && story.steps && story.steps[stepIndex]) {
            const step = story.steps[stepIndex];
            before = before || step.stateBefore;
            after = after || step.stateAfter;
          }
        }

        if (!before && !after) {
          console.log('No state data found for diff:', key);
          return;
        }

        try {
          const beforeObj = before ? JSON.parse(before) : {};
          const afterObj = after ? JSON.parse(after) : {};

          const differ = window.jsondiffpatch.create({
            objectHash: function(obj) {
              return obj.id || obj._id || obj.name;
            },
            arrays: {
              detectMove: true
            },
            textDiff: {
              minLength: 60
            }
          });

          const delta = differ.diff(beforeObj, afterObj);

          if (!delta) {
            container.innerHTML = '<div class="diff-line unchanged">No changes detected</div>';
            renderedDiffs[key] = true;
            return;
          }

          const html = window.htmlFormatter.format(delta, beforeObj);
          container.innerHTML = html;
          renderedDiffs[key] = true;
        } catch (e) {
          console.log('Failed to render diff for', key, ':', e.message);
          container.innerHTML = '<div class="text-gray-400">Unable to generate diff</div>';
          renderedDiffs[key] = true;
        }
      };

      /**
       * Renders before/after tree views
       */
      const renderTreeView = (key, before, after) => {
        // Try to get containers from refs, fall back to getElementById
        let beforeContainer = beforeTreeRefs[key];
        if (!beforeContainer) {
          const beforeId = 'tree-before-' + key;
          beforeContainer = document.getElementById(beforeId);
        }

        let afterContainer = afterTreeRefs[key];
        if (!afterContainer) {
          const afterId = 'tree-after-' + key;
          afterContainer = document.getElementById(afterId);
        }

        // If before/after not provided, try to get from embedded report JSON
        if ((!before || !after) && report.value && report.value.stories) {
          const [storyIndex, stepIndex] = key.split('-').map(Number);
          const story = report.value.stories[storyIndex];
          if (story && story.steps && story.steps[stepIndex]) {
            const step = story.steps[stepIndex];
            before = before || step.stateBefore;
            after = after || step.stateAfter;
          }
        }

        if (beforeContainer && before) {
          try {
            const data = JSON.parse(before);
            beforeContainer.innerHTML = '';
            if (window['w-jsonview-tree']) {
              window['w-jsonview-tree'](data, beforeContainer, { expanded: true });
            }
          } catch (e) {
            beforeContainer.innerHTML = '<pre class="text-gray-400">' + escapeHtml(before) + '</pre>';
          }
        }

        if (afterContainer && after) {
          try {
            const data = JSON.parse(after);
            afterContainer.innerHTML = '';
            if (window['w-jsonview-tree']) {
              window['w-jsonview-tree'](data, afterContainer, { expanded: true });
            }
          } catch (e) {
            afterContainer.innerHTML = '<pre class="text-gray-400">' + escapeHtml(after) + '</pre>';
          }
        }
      };

      // ============================================
      // UTILITY METHODS
      // ============================================

      const formatDuration = (ms) => {
        if (!ms) return '0ms';
        if (ms < 1000) return ms + 'ms';
        if (ms < 60000) return (ms / 1000).toFixed(1) + 's';
        return Math.floor(ms / 60000) + 'm ' + Math.floor((ms % 60000) / 1000) + 's';
      };

      const escapeHtml = (str) => {
        if (!str) return '';
        return String(str)
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/"/g, '&quot;')
          .replace(/'/g, '&#039;');
      };

      const getActionBadgeStyle = (actionType) => {
        if (!actionType) {
          return 'background-color: #581c87; color: #d8b4fe';
        }

        const cssClass = actionType.cssClass || '';
        if (cssClass === 'server') {
          return 'background-color: #1e3a5f; color: #93c5fd';
        }
        if (cssClass === 'client') {
          return 'background-color: #14532d; color: #bbf7d0';
        }
        return 'background-color: #581c87; color: #d8b4fe';
      };

      // ============================================
      // LIFECYCLE
      // ============================================
      onMounted(() => {
        // Hide loading spinner - Vue has mounted and will render content
        const loadingSpinner = document.getElementById('loading-spinner');
        if (loadingSpinner) {
          loadingSpinner.style.display = 'none';
        }

        // Parse report data from embedded JSON
        const dataElement = document.getElementById('report-data');
        if (dataElement) {
          try {
            report.value = JSON.parse(dataElement.textContent);
            stories.value = report.value.stories || [];

            // Initialize all stories as expanded (to show state comparisons)
            stories.value.forEach((_, index) => {
              expandedStories[index] = true;
            });

            // Render all diffs and responses after stories are expanded
            nextTick(() => {
              renderAllContent();
            });

            console.log('Report data loaded:', stories.value.length, 'stories');
          } catch (e) {
            console.error('Failed to parse report data:', e);
          }
        }

        // Listen for jsondiffpatch ready event
        window.addEventListener('jsondiffpatch-ready', () => {
          jsondiffpatchReady.value = true;
          console.log('jsondiffpatch is ready');

          // Re-render any pending diffs
          nextTick(() => {
            Object.keys(diffRefs).forEach(key => {
              if (!renderedDiffs[key]) {
                const [storyIndex, stepIndex] = key.split('-').map(Number);
                const story = stories.value[storyIndex];
                if (story && story.steps && story.steps[stepIndex]) {
                  const step = story.steps[stepIndex];
                  if (step.stateBefore || step.stateAfter) {
                    renderDiff(key, step.stateBefore, step.stateAfter);
                  }
                }
              }
            });
          });
        });

        // Check if jsondiffpatch is already available
        if (window.jsondiffpatch) {
          jsondiffpatchReady.value = true;
        }
      });

      // ============================================
      // RETURN PUBLIC API
      // ============================================
      return {
        // State
        report,
        stories,
        expandedStories,
        expandedResponses,

        // Computed
        totalSteps,
        passedSteps,
        failedSteps,

        // Methods
        toggleStory,
        expandAll,
        collapseAll,
        toggleResponse,
        setStateView,
        getStateView,
        formatDuration,
        getActionBadgeStyle,

        // Ref setters
        setResponseRef,
        setDiffRef,
        setBeforeTreeRef,
        setAfterTreeRef
      };
    }
  };

  // Create and mount the Vue application
  Vue.createApp(PilafReport).mount('#app');
})();